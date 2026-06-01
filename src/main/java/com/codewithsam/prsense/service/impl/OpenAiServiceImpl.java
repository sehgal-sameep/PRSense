package com.codewithsam.prsense.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.codewithsam.prsense.config.OpenAiProperties;
import com.codewithsam.prsense.model.FileDiff;
import com.codewithsam.prsense.model.LineComment;
import com.codewithsam.prsense.model.PrDetails;
import com.codewithsam.prsense.service.OpenAiService;
import com.codewithsam.prsense.util.DiffUtil;
import com.codewithsam.prsense.util.LanguageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAiServiceImpl implements OpenAiService {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate restTemplate;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    @Override
    public List<LineComment> reviewFile(PrDetails prDetails, FileDiff fileDiff, String crossFileContext) {
        String language = LanguageUtil.detect(fileDiff.getPath());
        String systemPrompt = buildSystemPrompt(language);
        String userMessage = buildUserMessage(prDetails, fileDiff, crossFileContext);

        log.info("Requesting AI review — file: {} [{}], {} lines",
                fileDiff.getPath(), language, fileDiff.getLineCount());

        Map<String, Object> requestBody = Map.of(
                "model", openAiProperties.getModel(),
                "max_tokens", openAiProperties.getMaxTokens(),
                "temperature", openAiProperties.getTemperature(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiProperties.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    OPENAI_CHAT_URL, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<>() {});
            return parseLineComments(response.getBody(), fileDiff.getPath());
        } catch (Exception e) {
            log.error("OpenAI API call failed for {}: {}", fileDiff.getPath(), e.getMessage());
            return List.of();
        }
    }

    // ------------------------------------------------------------------ //
    //  Response parsing
    // ------------------------------------------------------------------ //

    @SuppressWarnings("unchecked")
    private List<LineComment> parseLineComments(Map<?, ?> responseBody, String filePath) {
        if (responseBody == null) {
            log.warn("Null response body from OpenAI for: {}", filePath);
            return List.of();
        }

        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("No choices in OpenAI response for: {}", filePath);
                return List.of();
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                log.warn("No message in OpenAI response for: {}", filePath);
                return List.of();
            }

            String content = message.get("content").toString();
            // Strip markdown code fences in case the model wrapped the JSON anyway
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            List<LineComment> comments = objectMapper.readValue(content,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, LineComment.class));

            List<LineComment> valid = comments.stream()
                    .filter(c -> c.getLine() > 0 && c.getComment() != null && !c.getComment().trim().isEmpty())
                    .map(c -> new LineComment(c.getLine(), c.getComment().trim(), c.getSeverity(), c.getSuggestion()))
                    .toList();

            log.info("Parsed {} comment(s) for: {}", valid.size(), filePath);
            return valid;

        } catch (Exception e) {
            log.error("Failed to parse OpenAI response for {}: {}", filePath, e.getMessage());
            return List.of();
        }
    }

    // ------------------------------------------------------------------ //
    //  Prompt builders
    // ------------------------------------------------------------------ //

    // Builds the system prompt with language-specific rules injected for Java/Spring Boot
    private static String buildSystemPrompt(String language) {
        String langSection = "unknown".equals(language)
                ? ""
                : """

                  ## Language & Framework
                  Language: %s
                  Apply language-specific best practices for %s when reviewing.
                  """.formatted(language, language);

        String javaSection = "Java".equals(language) ? """

                  ## Java / Spring Boot Specific Rules
                  - Missing @Transactional — ONLY flag if the method performs MULTIPLE write \
operations (save/delete/update) that need atomicity. Do NOT flag single-write methods.
                  - N+1 query risks (fetching in loops)
                  - Incorrect use of Spring repositories
                  - Constructor injection preferred over field injection
                  - Custom exceptions preferred over RuntimeException
                  """ : "";

        return """
You are an expert code reviewer.
%s%s
## CRITICAL INSTRUCTION — OUTPUT FORMAT
Respond with a JSON array ONLY. No markdown, no text outside JSON.

[
  {
    "line": <line_number_in_AFTER_file>,
    "severity": "<LOW|MEDIUM|HIGH>",
    "comment": "<professional_review_comment>",
    "suggestion": "<short_code_snippet_showing_the_fix_or_null>"
  }
]

If no issues exist, return exactly: []

## FIELD RULES
- line      → 1-based line number from the AFTER file. Do NOT invent line numbers.
- severity  → HIGH (bugs/security), MEDIUM (quality/architecture), LOW (minor)
- comment   → Short, direct. Explain only when necessary.
- suggestion → A concrete code snippet (1–5 lines) showing exactly how to fix the issue.
               Use null if no specific fix can be shown (e.g. architectural advice).
               Do NOT repeat the existing code — show only the corrected version.

## REVIEW GUIDELINES
1. Bugs & Logic — null pointer risks, wrong conditions, off-by-one
2. Security — injection risks, missing validation, unsafe data handling
3. Code Quality — methods >40 lines, deep nesting, poor naming
4. Performance — inefficient loops, repeated calls, string concat in loops
5. Semantic Flow — trace enum values through method calls: if a method named \
`complete()` or `approve()` sets a status to `PROCESSING` instead of `COMPLETED`, \
flag it as a logic bug. Verify enum values match the method's name and post-condition.
6. Soft-delete bypass — flag when `findByIdAndIsDeletedFalse(...)` (or equivalent \
soft-delete-aware query) is replaced with plain `findById(...)`. This allows reads \
and writes to logically deleted records, which is a data-integrity bug.
7. Omissions — compare BEFORE vs AFTER to catch code that was silently removed: \
dropped annotations (@CascadeType, @NotNull, @Transactional, etc.), missing field \
assignments (e.g. `entity.setDatasetId(...)` present before but gone after), removed \
null-checks, and deleted validation logic. These are HIGH severity if they affect \
data integrity or correctness.

## COMMENT RULES
- Only flag real issues. Do NOT comment on good code.
- Combine multiple issues on the same line into one comment.
- HIGH severity only for critical issues.
- Write short, direct comments. Explain only when necessary.

FINAL REMINDER: Return ONLY the JSON array.
""".formatted(langSection, javaSection);
    }

    // Formats file content as numbered lines for the AI prompt; returns placeholder if empty
    private static String numberedLines(String content, String emptyPlaceholder) {
        if (content == null || content.isEmpty()) return emptyPlaceholder;
        String[] lines = content.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, 500); i++) {
            sb.append(String.format("%4d: %s%n", i + 1, lines[i]));
        }
        if (lines.length > 500) {
            sb.append("... (file truncated at 500 lines)");
        }
        return sb.toString();
    }

    // Builds the user message with before/after file content, unified diff, and cross-file context
    private static String buildUserMessage(PrDetails prDetails, FileDiff fileDiff, String crossFileContext) {
        String description = prDetails.getDescription() != null
                ? prDetails.getDescription()
                : "No description provided.";

        String before = fileDiff.getBefore() != null ? fileDiff.getBefore() : "";
        String after = fileDiff.getAfter() != null && !fileDiff.getAfter().isEmpty()
                ? fileDiff.getAfter()
                : "";

        // Unified diff — shows exactly what changed with context lines
        String unifiedDiff = DiffUtil.buildUnifiedDiff(before, after, fileDiff.getPath());

        // Full "before" with line numbers — lets AI spot omissions (removed annotations, dropped fields, etc.)
        String beforeContent = numberedLines(before, "// File did not exist before this PR");

        // Full "after" with line numbers — used by AI to anchor comment line numbers
        String afterContent = numberedLines(after, "// File was deleted in this PR");

        String contextSection = (crossFileContext != null && !crossFileContext.isBlank())
                ? "\n--- OTHER FILES CHANGED IN THIS PR ---\n" + crossFileContext + "\n"
                : "";

        return String.format("""
        Review this single file change from a pull request.

        PR Title: %s
        PR Description: %s
        File: %s [changeType: %s]
        %s
        --- FULL FILE BEFORE CHANGE (what existed before) ---
        %s

        --- UNIFIED DIFF (what changed) ---
        %s

        --- FULL FILE AFTER CHANGE (use these line numbers for your comments) ---
        %s

        IMPORTANT:
        - Compare BEFORE vs AFTER carefully to catch omissions: code removed without a replacement,
          dropped annotations (@CascadeType, @Transactional, etc.), missing field assignments, etc.
        - Line numbers in your JSON must come from the "FULL FILE AFTER CHANGE" section above.
        - Respond ONLY with a JSON array. No markdown, no explanation outside JSON.
        """,
                prDetails.getTitle(),
                description,
                fileDiff.getPath(),
                fileDiff.getChangeType(),
                contextSection,
                beforeContent,
                unifiedDiff,
                afterContent
        );
    }
}
