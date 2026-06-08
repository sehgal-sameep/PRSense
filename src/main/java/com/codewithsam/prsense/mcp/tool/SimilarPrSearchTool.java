package com.codewithsam.prsense.mcp.tool;

import com.codewithsam.prsense.mcp.constants.ToolNames;
import com.codewithsam.prsense.mcp.dto.SearchSimilarPrsRequest;
import com.codewithsam.prsense.mcp.exception.ToolExecutionException;
import com.codewithsam.prsense.mcp.response.SimilarPrResult;
import com.codewithsam.prsense.mcp.service.ReviewHistoryToolService;
import com.codewithsam.prsense.mcp.service.ToolMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimilarPrSearchTool {

    private final ReviewHistoryToolService reviewHistoryToolService;
    private final ToolMetricsService toolMetricsService;

    @Tool(
        name = ToolNames.SEARCH_SIMILAR_PRS,
        description = "Finds historically reviewed PRs that are semantically similar to the given query. " +
                      "Use this to surface past decisions, patterns, or bugs that are relevant to the current change."
    )
    public SimilarPrResult searchSimilarPrs(SearchSimilarPrsRequest request) {
        long start = System.currentTimeMillis();
        toolMetricsService.recordToolStart(ToolNames.SEARCH_SIMILAR_PRS, request.getQuery());
        try {
            SimilarPrResult result = reviewHistoryToolService.searchSimilarPrs(request);
            toolMetricsService.recordToolSuccess(ToolNames.SEARCH_SIMILAR_PRS,
                    request.getQuery(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            toolMetricsService.recordToolFailure(ToolNames.SEARCH_SIMILAR_PRS,
                    request.getQuery(), e.getClass().getSimpleName());
            throw new ToolExecutionException(ToolNames.SEARCH_SIMILAR_PRS, e.getMessage(), e);
        }
    }
}
