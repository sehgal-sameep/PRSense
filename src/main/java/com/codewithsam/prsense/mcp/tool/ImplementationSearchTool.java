package com.codewithsam.prsense.mcp.tool;

import com.codewithsam.prsense.mcp.constants.ToolNames;
import com.codewithsam.prsense.mcp.dto.FindImplementationsRequest;
import com.codewithsam.prsense.mcp.exception.ToolExecutionException;
import com.codewithsam.prsense.mcp.response.ImplementationResult;
import com.codewithsam.prsense.mcp.service.ReferenceSearchService;
import com.codewithsam.prsense.mcp.service.ToolMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImplementationSearchTool {

    private final ReferenceSearchService referenceSearchService;
    private final ToolMetricsService toolMetricsService;

    @Tool(
        name = ToolNames.FIND_IMPLEMENTATIONS,
        description = "Finds all concrete classes that implement a given interface. " +
                      "Returns class name, file path, and package for each implementation. " +
                      "Use this to detect missing implementations or contract violations."
    )
    public ImplementationResult findImplementations(FindImplementationsRequest request) {
        long start = System.currentTimeMillis();
        toolMetricsService.recordToolStart(ToolNames.FIND_IMPLEMENTATIONS, request.getInterfaceName());
        try {
            ImplementationResult result = referenceSearchService.findImplementations(request);
            toolMetricsService.recordToolSuccess(ToolNames.FIND_IMPLEMENTATIONS,
                    request.getInterfaceName(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            toolMetricsService.recordToolFailure(ToolNames.FIND_IMPLEMENTATIONS,
                    request.getInterfaceName(), e.getClass().getSimpleName());
            throw new ToolExecutionException(ToolNames.FIND_IMPLEMENTATIONS, e.getMessage(), e);
        }
    }
}
