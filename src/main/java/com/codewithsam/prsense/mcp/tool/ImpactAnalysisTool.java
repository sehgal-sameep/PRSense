package com.codewithsam.prsense.mcp.tool;

import com.codewithsam.prsense.mcp.constants.ToolNames;
import com.codewithsam.prsense.mcp.dto.AnalyzeImpactRequest;
import com.codewithsam.prsense.mcp.exception.ToolExecutionException;
import com.codewithsam.prsense.mcp.response.ImpactAnalysisResult;
import com.codewithsam.prsense.mcp.service.KnowledgeGraphService;
import com.codewithsam.prsense.mcp.service.ToolMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImpactAnalysisTool {

    private final KnowledgeGraphService knowledgeGraphService;
    private final ToolMetricsService toolMetricsService;

    @Tool(
        name = ToolNames.ANALYZE_IMPACT,
        description = "Analyzes the blast radius of changing a given class. " +
                      "Returns all controllers, services, repositories, scheduled jobs, and event listeners " +
                      "that directly or indirectly depend on it. " +
                      "Use this before flagging a high-risk change in a PR review."
    )
    public ImpactAnalysisResult analyzeImpact(AnalyzeImpactRequest request) {
        long start = System.currentTimeMillis();
        toolMetricsService.recordToolStart(ToolNames.ANALYZE_IMPACT, request.getClassName());
        try {
            ImpactAnalysisResult result = knowledgeGraphService.analyzeImpact(request);
            toolMetricsService.recordToolSuccess(ToolNames.ANALYZE_IMPACT,
                    request.getClassName(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            toolMetricsService.recordToolFailure(ToolNames.ANALYZE_IMPACT,
                    request.getClassName(), e.getClass().getSimpleName());
            throw new ToolExecutionException(ToolNames.ANALYZE_IMPACT, e.getMessage(), e);
        }
    }
}
