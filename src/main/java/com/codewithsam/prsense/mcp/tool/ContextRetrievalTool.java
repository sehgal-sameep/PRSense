package com.codewithsam.prsense.mcp.tool;

import com.codewithsam.prsense.mcp.constants.ToolNames;
import com.codewithsam.prsense.mcp.dto.RetrieveContextRequest;
import com.codewithsam.prsense.mcp.exception.ToolExecutionException;
import com.codewithsam.prsense.mcp.response.ContextRetrievalResult;
import com.codewithsam.prsense.mcp.service.McpContextRetrievalService;
import com.codewithsam.prsense.mcp.service.ToolMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContextRetrievalTool {

    private final McpContextRetrievalService contextRetrievalService;
    private final ToolMetricsService toolMetricsService;

    @Tool(
        name = ToolNames.RETRIEVE_CONTEXT,
        description = "One-stop context retrieval for a pull request. " +
                      "Internally calls search_code, find_references, get_related_code, and search_review_history " +
                      "and returns an aggregated context bundle. " +
                      "Use this as the first tool call when starting a PR review to get full situational awareness."
    )
    public ContextRetrievalResult retrieveContext(RetrieveContextRequest request) {
        long start = System.currentTimeMillis();
        String contextId = "PR#" + request.getPullRequestId();
        toolMetricsService.recordToolStart(ToolNames.RETRIEVE_CONTEXT, contextId);
        try {
            ContextRetrievalResult result = contextRetrievalService.retrieveContext(request);
            toolMetricsService.recordToolSuccess(ToolNames.RETRIEVE_CONTEXT,
                    contextId, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            toolMetricsService.recordToolFailure(ToolNames.RETRIEVE_CONTEXT,
                    contextId, e.getClass().getSimpleName());
            throw new ToolExecutionException(ToolNames.RETRIEVE_CONTEXT, e.getMessage(), e);
        }
    }
}
