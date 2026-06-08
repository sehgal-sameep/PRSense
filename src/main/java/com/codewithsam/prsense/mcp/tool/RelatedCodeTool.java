package com.codewithsam.prsense.mcp.tool;

import com.codewithsam.prsense.mcp.constants.ToolNames;
import com.codewithsam.prsense.mcp.dto.GetRelatedCodeRequest;
import com.codewithsam.prsense.mcp.exception.ToolExecutionException;
import com.codewithsam.prsense.mcp.response.RelatedCodeResult;
import com.codewithsam.prsense.mcp.service.KnowledgeGraphService;
import com.codewithsam.prsense.mcp.service.ToolMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RelatedCodeTool {

    private final KnowledgeGraphService knowledgeGraphService;
    private final ToolMetricsService toolMetricsService;

    @Tool(
        name = ToolNames.GET_RELATED_CODE,
        description = "Retrieves semantically related components for a given class. " +
                      "Uses the repository dependency graph to find services, repositories, " +
                      "controllers, DTOs, and entities that interact with the target class. " +
                      "Use this to build a full picture of a class's dependencies before reviewing changes."
    )
    public RelatedCodeResult getRelatedCode(GetRelatedCodeRequest request) {
        long start = System.currentTimeMillis();
        toolMetricsService.recordToolStart(ToolNames.GET_RELATED_CODE, request.getClassName());
        try {
            RelatedCodeResult result = knowledgeGraphService.getRelatedCode(request);
            toolMetricsService.recordToolSuccess(ToolNames.GET_RELATED_CODE,
                    request.getClassName(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            toolMetricsService.recordToolFailure(ToolNames.GET_RELATED_CODE,
                    request.getClassName(), e.getClass().getSimpleName());
            throw new ToolExecutionException(ToolNames.GET_RELATED_CODE, e.getMessage(), e);
        }
    }
}
