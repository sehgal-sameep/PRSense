package com.codewithsam.prsense.mcp.tool;

import com.codewithsam.prsense.mcp.constants.ToolNames;
import com.codewithsam.prsense.mcp.dto.GetClassHierarchyRequest;
import com.codewithsam.prsense.mcp.exception.ToolExecutionException;
import com.codewithsam.prsense.mcp.response.ClassHierarchyResult;
import com.codewithsam.prsense.mcp.service.KnowledgeGraphService;
import com.codewithsam.prsense.mcp.service.ToolMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClassHierarchyTool {

    private final KnowledgeGraphService knowledgeGraphService;
    private final ToolMetricsService toolMetricsService;

    @Tool(
        name = ToolNames.GET_CLASS_HIERARCHY,
        description = "Returns the full inheritance and implementation tree for a class. " +
                      "Includes parent classes, implemented interfaces, child classes, and child interfaces. " +
                      "Use this to understand polymorphism and detect Liskov Substitution Principle violations."
    )
    public ClassHierarchyResult getClassHierarchy(GetClassHierarchyRequest request) {
        long start = System.currentTimeMillis();
        toolMetricsService.recordToolStart(ToolNames.GET_CLASS_HIERARCHY, request.getClassName());
        try {
            ClassHierarchyResult result = knowledgeGraphService.getClassHierarchy(request);
            toolMetricsService.recordToolSuccess(ToolNames.GET_CLASS_HIERARCHY,
                    request.getClassName(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            toolMetricsService.recordToolFailure(ToolNames.GET_CLASS_HIERARCHY,
                    request.getClassName(), e.getClass().getSimpleName());
            throw new ToolExecutionException(ToolNames.GET_CLASS_HIERARCHY, e.getMessage(), e);
        }
    }
}
