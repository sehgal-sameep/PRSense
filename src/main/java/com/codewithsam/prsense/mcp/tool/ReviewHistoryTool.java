package com.codewithsam.prsense.mcp.tool;

import com.codewithsam.prsense.mcp.constants.ToolNames;
import com.codewithsam.prsense.mcp.dto.SearchReviewHistoryRequest;
import com.codewithsam.prsense.mcp.exception.ToolExecutionException;
import com.codewithsam.prsense.mcp.response.ReviewHistoryResult;
import com.codewithsam.prsense.mcp.service.ReviewHistoryToolService;
import com.codewithsam.prsense.mcp.service.ToolMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewHistoryTool {

    private final ReviewHistoryToolService reviewHistoryToolService;
    private final ToolMetricsService toolMetricsService;

    @Tool(
        name = ToolNames.SEARCH_REVIEW_HISTORY,
        description = "Searches historical AI review records for a repository. " +
                      "Use this to find past issues, patterns, or recurring problems. " +
                      "Returns review ID, PR ID, file counts, and comment statistics."
    )
    public ReviewHistoryResult searchReviewHistory(SearchReviewHistoryRequest request) {
        long start = System.currentTimeMillis();
        toolMetricsService.recordToolStart(ToolNames.SEARCH_REVIEW_HISTORY, request.getQuery());
        try {
            ReviewHistoryResult result = reviewHistoryToolService.searchHistory(request);
            toolMetricsService.recordToolSuccess(ToolNames.SEARCH_REVIEW_HISTORY,
                    request.getQuery(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            toolMetricsService.recordToolFailure(ToolNames.SEARCH_REVIEW_HISTORY,
                    request.getQuery(), e.getClass().getSimpleName());
            throw new ToolExecutionException(ToolNames.SEARCH_REVIEW_HISTORY, e.getMessage(), e);
        }
    }
}
