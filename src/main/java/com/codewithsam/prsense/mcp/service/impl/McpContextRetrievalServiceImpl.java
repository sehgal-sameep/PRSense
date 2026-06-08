package com.codewithsam.prsense.mcp.service.impl;

import com.codewithsam.prsense.mcp.dto.*;
import com.codewithsam.prsense.mcp.exception.ContextRetrievalException;
import com.codewithsam.prsense.mcp.response.*;
import com.codewithsam.prsense.mcp.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// Aggregates all MCP tools into a single context bundle for a given PR.
// This is the one-stop retrieval the agent calls when it needs full situational awareness.
@Service
@RequiredArgsConstructor
@Slf4j
public class McpContextRetrievalServiceImpl implements McpContextRetrievalService {

    private final CodeSearchToolService codeSearchToolService;
    private final ReviewHistoryToolService reviewHistoryToolService;
    private final KnowledgeGraphService knowledgeGraphService;

    @Override
    public ContextRetrievalResult retrieveContext(RetrieveContextRequest request) {
        log.info("Context retrieval started — PR #{}, repository: '{}'",
                request.getPullRequestId(), request.getRepository());

        try {
            // 1. Semantic search for code relevant to the changed files
            String searchQuery = buildSearchQuery(request);
            CodeSearchResult codeMatches = codeSearchToolService.search(
                    SearchCodeRequest.builder()
                            .query(searchQuery)
                            .repository(request.getRepository())
                            .build());

            // 2. Historical reviews for context
            ReviewHistoryResult history = reviewHistoryToolService.searchHistory(
                    SearchReviewHistoryRequest.builder()
                            .query(request.getRepository())
                            .repositoryId(request.getRepository())
                            .limit(5)
                            .build());

            // 3. Related classes for each changed file
            List<String> relatedClasses = new ArrayList<>();
            if (request.getChangedFiles() != null) {
                for (String file : request.getChangedFiles()) {
                    String className = extractClassName(file);
                    if (className != null) {
                        RelatedCodeResult related = knowledgeGraphService.getRelatedCode(
                                GetRelatedCodeRequest.builder()
                                        .className(className)
                                        .repository(request.getRepository())
                                        .build());
                        related.getServices().forEach(c -> relatedClasses.add(c.getClassName()));
                        related.getRepositories().forEach(c -> relatedClasses.add(c.getClassName()));
                    }
                }
            }

            // 4. Build a compact aggregated context string for direct LLM consumption
            String aggregated = buildAggregatedContext(codeMatches, relatedClasses);

            log.info("Context retrieval completed — PR #{}, code matches: {}, related classes: {}",
                    request.getPullRequestId(), codeMatches.getTotalResults(), relatedClasses.size());

            return ContextRetrievalResult.builder()
                    .pullRequestId(request.getPullRequestId())
                    .repository(request.getRepository())
                    .relevantCode(codeMatches.getMatches())
                    .historicalReviews(history.getReviews())
                    .relatedClasses(relatedClasses.stream().distinct().toList())
                    .aggregatedContext(aggregated)
                    .build();

        } catch (Exception e) {
            log.error("Context retrieval failed — PR #{}: {}", request.getPullRequestId(), e.getMessage(), e);
            throw new ContextRetrievalException(
                    "Failed to retrieve context for PR #" + request.getPullRequestId(), e);
        }
    }

    private String buildSearchQuery(RetrieveContextRequest request) {
        if (request.getChangedFiles() != null && !request.getChangedFiles().isEmpty()) {
            // Extract class names for better semantic matching
            List<String> classNames = request.getChangedFiles().stream()
                    .map(this::extractClassName)
                    .filter(c -> c != null && !c.isBlank())
                    .distinct()
                    .toList();
            if (!classNames.isEmpty()) {
                String q = String.join(" ", classNames) + " business logic implementation";
                log.debug("Semantic search query: {} (from {} file(s))", q, classNames.size());
                return q;
            }
            return "code related to " + String.join(", ", request.getChangedFiles());
        }
        log.debug("No file names -- using generic fallback query");
        return "core business logic and service layer";
    }

    // Extracts a class name from a file path like "src/main/.../UserService.java"
    private String extractClassName(String filePath) {
        if (filePath == null) return null;
        String[] parts = filePath.replace("\\", "/").split("/");
        String fileName = parts[parts.length - 1];
        return fileName.endsWith(".java") ? fileName.replace(".java", "") : null;
    }

    private String buildAggregatedContext(CodeSearchResult codeMatches, List<String> relatedClasses) {
        StringBuilder sb = new StringBuilder("## Repository Context\n\n");

        if (!codeMatches.getMatches().isEmpty()) {
            sb.append("### Relevant Code\n");
            codeMatches.getMatches().forEach(m ->
                    sb.append("- `").append(m.getClassName()).append(".")
                      .append(m.getMethodName() != null ? m.getMethodName() : "").append("`")
                      .append(" (").append(m.getFilePath()).append(")\n"));
        }

        if (!relatedClasses.isEmpty()) {
            sb.append("\n### Related Components\n");
            relatedClasses.stream().distinct().forEach(c -> sb.append("- ").append(c).append("\n"));
        }

        return sb.toString();
    }
}
