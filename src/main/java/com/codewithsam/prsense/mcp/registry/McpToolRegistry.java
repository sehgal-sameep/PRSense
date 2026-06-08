package com.codewithsam.prsense.mcp.registry;

import com.codewithsam.prsense.mcp.tool.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Registers all MCP tools with the Spring AI MCP server.
// The ToolCallbackProvider bean is discovered automatically by Spring AI's MCP auto-configuration.
// To add a new tool: implement the tool class and add it here — no other wiring needed.
@Configuration
@Slf4j
public class McpToolRegistry {

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(
            CodeSearchTool codeSearchTool,
            FileContentTool fileContentTool,
            ReferenceSearchTool referenceSearchTool,
            ImplementationSearchTool implementationSearchTool,
            ClassHierarchyTool classHierarchyTool,
            RelatedCodeTool relatedCodeTool,
            ReviewHistoryTool reviewHistoryTool,
            SimilarPrSearchTool similarPrSearchTool,
            ImpactAnalysisTool impactAnalysisTool,
            ContextRetrievalTool contextRetrievalTool) {

        log.info("MCP tool registry — building ToolCallbackProvider with 10 tools");

        ToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(
                        codeSearchTool,
                        fileContentTool,
                        referenceSearchTool,
                        implementationSearchTool,
                        classHierarchyTool,
                        relatedCodeTool,
                        reviewHistoryTool,
                        similarPrSearchTool,
                        impactAnalysisTool,
                        contextRetrievalTool)
                .build();

        // Log each registered tool name so startup logs prove registration succeeded
        int count = provider.getToolCallbacks().length;
        for (var cb : provider.getToolCallbacks()) {
            log.info("MCP tool registered — name: '{}', description length: {} chars",
                    cb.getToolDefinition().name(),
                    cb.getToolDefinition().description().length());
        }
        log.info("MCP tool registry initialized — {} tool(s) registered and attached to ChatClient",
                count);

        return provider;
    }
}
