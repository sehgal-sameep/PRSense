package com.codewithsam.prsense.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.codewithsam.prsense.model.FileDiff;
import com.codewithsam.prsense.model.LineComment;
import com.codewithsam.prsense.model.PrDetails;
import com.codewithsam.prsense.service.AgentReviewService;
import com.codewithsam.prsense.util.DiffUtil;
import com.codewithsam.prsense.util.LanguageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.List;

// Replaces the static prompt-only flow. Uses Spring AI ChatClient with all 10 MCP tools
// attached so the LLM can autonomously call tools before generating the final review.
// The presence of "Tool execution started" logs confirms agentic behaviour is active.
@Service
@Slf4j
public class AgentReviewServiceImpl implements AgentReviewService {

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;
    private final ObjectMapper objectMapper;

    public AgentReviewServiceImpl(ChatClient.Builder chatClientBuilder,
                                  ToolCallbackProvider toolCallbackProvider,
                                  ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.toolCallbackProvider = toolCallbackProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<LineComment> reviewFile(PrDetails prDetails, FileDiff fileDiff,
                                        String crossFileContext, String repository,
                                        List<String> allChangedFilePaths) {
        String language = LanguageUtil.detect(fileDiff.getPath());
        String systemPrompt = buildAgentSystemPrompt(language);
        String userMessage  = buildUserMessage(prDetails, fileDiff, crossFileContext, repository, allChangedFilePaths);

        int toolCount = toolCallbackProvider.getToolCallbacks().length;
        log.info("Agent review dispatched — file: {} [{}], {} lines, {} tools attached, repository: '{}'",
                fileDiff.getPath(), language, fileDiff.getLineCount(), toolCount, repository);
        log.debug("Agent system prompt length: {} chars", systemPrompt.length());

        long start = System.currentTimeMillis();
        String rawResponse;
        try {
            // This single call may trigger multiple LLM ↔ tool round trips internally.
            // Spring AI handles the tool-call loop: tool_calls → execute → feed result → repeat → stop.
            rawResponse = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Agent review failed for '{}': {}", fileDiff.getPath(), e.getMessage(), e);
            return List.of();
        }

        long durationMs = System.currentTimeMillis() - start;
        log.info("Agent review response received — file: '{}', duration: {}ms, response_length: {}",
                fileDiff.getPath(), durationMs, rawResponse != null ? rawResponse.length() : 0);

        return parseLineComments(rawResponse, fileDiff.getPath());
    }

    // ------------------------------------------------------------------ //
    //  Prompt builders
    // ------------------------------------------------------------------ //

    private static String buildAgentSystemPrompt(String language) {
        String langSection = "unknown".equals(language) ? ""
                : "\n## Language\nLanguage: %s. Apply %s best practices.\n".formatted(language, language);

        String javaSection = "Java".equals(language) ? """

                ## Java / Spring Boot Rules
                - Missing @Transactional only if method does MULTIPLE writes needing atomicity
                - N+1 query risks (fetching in loops)
                - Constructor injection preferred over field injection
                - Custom exceptions preferred over RuntimeException
                """ : "";

        return """
You are an expert code reviewer. Your PRIMARY job is to review the provided code diff.

## CRITICAL — DIFF-FIRST REVIEW
The diff and full file content below ARE your primary source of truth.
Review them thoroughly regardless of what tools return.
Do NOT return an empty result just because tool calls returned no data.
Tools are SUPPLEMENTARY. Empty tool results mean the repository index is not built yet.
Even without tool context, you MUST review the diff for real issues.
Return [] ONLY when the diff genuinely has zero bugs, zero risks, and zero quality issues.

## TOOLS (SUPPLEMENTARY — use only when needed)
- retrieve_context  → pass the list of changed file names to get full PR context
- search_code       → find similar patterns in the codebase
- find_references   → all usages of a symbol you see changing
- find_implementations → classes implementing a changed interface
- get_related_code  → related services, repos, DTOs for a changed class
- get_class_hierarchy → inheritance tree for impact assessment
- analyze_impact    → components affected by changing a class
- search_review_history → past recurring issues in this repository

Skip tools when the issue is obvious from the diff alone.
%s%s
## OUTPUT FORMAT — MANDATORY
Return a JSON array ONLY. No markdown. No text outside the array.

[
  {
    "line": <1-based line number from AFTER file>,
    "severity": "<HIGH|MEDIUM|LOW>",
    "comment": "<concise review comment>",
    "suggestion": "<short fix snippet or null>"
  }
]

If no issues found: []

## SEVERITY RULES
- HIGH   → bugs, security issues, data integrity violations
- MEDIUM → architecture, code quality, missing validations
- LOW    → style, naming, minor improvements

## REVIEW GUIDELINES
1. Bugs & Logic — null pointer risks, wrong conditions, off-by-one
2. Security — injection, missing validation, unsafe data handling
3. Code Quality — methods >40 lines, deep nesting, poor naming
4. Performance — inefficient loops, repeated calls, string concat in loops
5. Semantic Flow — verify enum values match the method's semantic intent
6. Soft-delete bypass — findById() replacing findByIdAndIsDeletedFalse()
7. Omissions — dropped @Transactional, removed null checks, missing field assignments

Only flag real issues. Combine multi-issue same-line into one comment.
FINAL REMINDER: Return ONLY the JSON array after all tool calls.
""".formatted(langSection, javaSection);
    }

    private static String buildUserMessage(PrDetails prDetails, FileDiff fileDiff,
                                           String crossFileContext, String repository,
                                        List<String> allChangedFilePaths) {
        String description = prDetails.getDescription() != null
                ? prDetails.getDescription() : "No description provided.";
        String before = fileDiff.getBefore() != null ? fileDiff.getBefore() : "";
        String after  = fileDiff.getAfter()  != null ? fileDiff.getAfter()  : "";
        String diff   = DiffUtil.buildUnifiedDiff(before, after, fileDiff.getPath());

        String contextSection = (crossFileContext != null && !crossFileContext.isBlank())
                ? "\n--- OTHER FILES IN THIS PR ---\n" + crossFileContext + "\n" : "";

        // Explicit file list helps the agent pass concrete names to retrieve_context
        String changedFilesHint = "";
        if (allChangedFilePaths != null && !allChangedFilePaths.isEmpty()) {
            changedFilesHint = "\nAll changed files in this PR (pass to retrieve_context if needed):\n"
                    + String.join("\n", allChangedFilePaths) + "\n";
        }

        return """
        Review this code change. Repository: %s
        PR Title: %s
        PR Description: %s
        File being reviewed: %s [changeType: %s]
        %s%s
        --- FULL FILE BEFORE (what existed before this PR) ---
        %s

        --- UNIFIED DIFF (what changed) ---
        %s

        --- FULL FILE AFTER (use THESE line numbers in your JSON comments) ---
        %s

        INSTRUCTIONS:
        1. Review the diff above carefully for ALL real issues.
        2. Optionally call retrieve_context with the changed files above for broader context.
        3. Return ONLY the JSON array. No explanations outside JSON.
        """.formatted(
                repository,
                prDetails.getTitle(), description,
                fileDiff.getPath(), fileDiff.getChangeType(),
                changedFilesHint,
                contextSection,
                numberedLines(before, "// File did not exist before this PR"),
                diff,
                numberedLines(after, "// File was deleted in this PR"));
    }

    // ------------------------------------------------------------------ //
    //  Response parsing
    // ------------------------------------------------------------------ //

    @SuppressWarnings("unchecked")
    private List<LineComment> parseLineComments(String rawContent, String filePath) {
        if (rawContent == null || rawContent.isBlank()) {
            log.warn("Empty response from agent for: {}", filePath);
            return List.of();
        }
        try {
            String content = rawContent
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            // If the LLM returned an empty array shorthand
            if ("[]".equals(content)) return List.of();

            List<LineComment> comments = objectMapper.readValue(content,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, LineComment.class));

            List<LineComment> valid = comments.stream()
                    .filter(c -> c.getLine() > 0 && c.getComment() != null && !c.getComment().isBlank())
                    .map(c -> new LineComment(c.getLine(), c.getComment().trim(),
                            c.getSeverity(), c.getSuggestion()))
                    .toList();

            log.info("Agent parsed {} comment(s) for: {}", valid.size(), filePath);
            return valid;
        } catch (Exception e) {
            log.error("Failed to parse agent response for '{}': {} | raw: {}",
                    filePath, e.getMessage(),
                    rawContent.length() > 200 ? rawContent.substring(0, 200) + "…" : rawContent);
            return List.of();
        }
    }

    private static String numberedLines(String content, String placeholder) {
        if (content == null || content.isBlank()) return placeholder;
        String[] lines = content.split("\n");
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(lines.length, 500);
        for (int i = 0; i < limit; i++) {
            sb.append(String.format("%4d: %s%n", i + 1, lines[i]));
        }
        if (lines.length > 500) sb.append("... (truncated at 500 lines)");
        return sb.toString();
    }
}
