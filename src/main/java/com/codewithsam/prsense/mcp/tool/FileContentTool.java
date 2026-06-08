package com.codewithsam.prsense.mcp.tool;

import com.codewithsam.prsense.mcp.constants.ToolNames;
import com.codewithsam.prsense.mcp.dto.GetFileRequest;
import com.codewithsam.prsense.mcp.exception.ToolExecutionException;
import com.codewithsam.prsense.mcp.response.FileContentResult;
import com.codewithsam.prsense.mcp.service.FileContentToolService;
import com.codewithsam.prsense.mcp.service.ToolMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileContentTool {

    private final FileContentToolService fileContentToolService;
    private final ToolMetricsService toolMetricsService;

    @Tool(
        name = ToolNames.GET_FILE,
        description = "Retrieves the content of a source file from the repository. " +
                      "Supports optional startLine and endLine for targeted line range retrieval. " +
                      "Falls back to indexed chunk content if the file is not on the local filesystem."
    )
    public FileContentResult getFile(GetFileRequest request) {
        long start = System.currentTimeMillis();
        toolMetricsService.recordToolStart(ToolNames.GET_FILE, request.getPath());
        try {
            FileContentResult result = fileContentToolService.getFileContent(request);
            toolMetricsService.recordToolSuccess(ToolNames.GET_FILE,
                    request.getPath(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            toolMetricsService.recordToolFailure(ToolNames.GET_FILE,
                    request.getPath(), e.getClass().getSimpleName());
            throw new ToolExecutionException(ToolNames.GET_FILE, e.getMessage(), e);
        }
    }
}
