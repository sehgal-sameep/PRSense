package com.codewithsam.prsense.mcp.tool;

import com.codewithsam.prsense.mcp.constants.ToolNames;
import com.codewithsam.prsense.mcp.dto.FindReferencesRequest;
import com.codewithsam.prsense.mcp.exception.ToolExecutionException;
import com.codewithsam.prsense.mcp.response.ReferenceResult;
import com.codewithsam.prsense.mcp.service.ReferenceSearchService;
import com.codewithsam.prsense.mcp.service.ToolMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReferenceSearchTool {

    private final ReferenceSearchService referenceSearchService;
    private final ToolMetricsService toolMetricsService;

    @Tool(
        name = ToolNames.FIND_REFERENCES,
        description = "Finds all usages of a given symbol (class, method, or field name) across the repository. " +
                      "Returns the file, class, and method where each reference appears. " +
                      "Use this to understand the impact of changing a symbol."
    )
    public ReferenceResult findReferences(FindReferencesRequest request) {
        long start = System.currentTimeMillis();
        toolMetricsService.recordToolStart(ToolNames.FIND_REFERENCES, request.getSymbol());
        try {
            ReferenceResult result = referenceSearchService.findReferences(request);
            toolMetricsService.recordToolSuccess(ToolNames.FIND_REFERENCES,
                    request.getSymbol(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            toolMetricsService.recordToolFailure(ToolNames.FIND_REFERENCES,
                    request.getSymbol(), e.getClass().getSimpleName());
            throw new ToolExecutionException(ToolNames.FIND_REFERENCES, e.getMessage(), e);
        }
    }
}
