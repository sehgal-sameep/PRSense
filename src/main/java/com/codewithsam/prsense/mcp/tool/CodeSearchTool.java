package com.codewithsam.prsense.mcp.tool;

import com.codewithsam.prsense.mcp.constants.ToolNames;
import com.codewithsam.prsense.mcp.dto.SearchCodeRequest;
import com.codewithsam.prsense.mcp.exception.ToolExecutionException;
import com.codewithsam.prsense.mcp.response.CodeSearchResult;
import com.codewithsam.prsense.mcp.service.CodeSearchToolService;
import com.codewithsam.prsense.mcp.service.ToolMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeSearchTool {

    private final CodeSearchToolService codeSearchToolService;
    private final ToolMetricsService toolMetricsService;

    @Tool(
        name = ToolNames.SEARCH_CODE,
        description = "Performs semantic similarity search over indexed repository code. " +
                      "Use this to find relevant classes, methods, or logic by natural language query. " +
                      "Returns file paths, class names, method names, and content snippets."
    )
    public CodeSearchResult searchCode(SearchCodeRequest request) {
        long start = System.currentTimeMillis();
        toolMetricsService.recordToolStart(ToolNames.SEARCH_CODE, request.getRepository());
        try {
            CodeSearchResult result = codeSearchToolService.search(request);
            toolMetricsService.recordToolSuccess(ToolNames.SEARCH_CODE,
                    request.getRepository(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            toolMetricsService.recordToolFailure(ToolNames.SEARCH_CODE,
                    request.getRepository(), e.getClass().getSimpleName());
            throw new ToolExecutionException(ToolNames.SEARCH_CODE, e.getMessage(), e);
        }
    }
}
