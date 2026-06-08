package com.codewithsam.prsense.mcp.graph;

import com.codewithsam.prsense.constants.SymbolType;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Builds a RepositoryGraph from indexed CodeChunkEntity data using lightweight pattern matching.
// No full JavaParser re-run — relationships are extracted from the stored chunk content.
@Component
@Slf4j
public class RepositoryGraphBuilder {

    private static final Pattern EXTENDS_PATTERN =
            Pattern.compile("\\bextends\\s+(\\w+)");
    private static final Pattern IMPLEMENTS_PATTERN =
            Pattern.compile("\\bimplements\\s+([\\w,\\s]+?)(?:\\{|$)");
    private static final Pattern FIELD_PATTERN =
            Pattern.compile("(?:private|protected|public)\\s+(?:final\\s+)?(\\w+)\\s+\\w+\\s*[;=]");
    private static final Pattern METHOD_CALL_PATTERN =
            Pattern.compile("(\\w+)\\.\\w+\\(");

    public RepositoryGraph build(String repository, List<CodeChunkEntity> chunks) {
        log.info("Building repository graph — repository: '{}', chunks: {}", repository, chunks.size());
        RepositoryGraph graph = new RepositoryGraph();

        // Pass 1: Add all class/interface/enum/record nodes
        for (CodeChunkEntity chunk : chunks) {
            if (isTypeDeclaration(chunk.getSymbolType())) {
                RepositoryNode node = RepositoryNode.builder()
                        .id(chunk.getClassName())
                        .simpleName(chunk.getClassName())
                        .type(NodeType.from(chunk.getSymbolType()))
                        .filePath(chunk.getFilePath())
                        .packageName(chunk.getPackageName())
                        .repository(repository)
                        .build();
                graph.addNode(node);
            }
        }

        // Pass 2: Add method nodes and extract edges from content
        for (CodeChunkEntity chunk : chunks) {
            if (chunk.getSymbolType() == SymbolType.METHOD || chunk.getSymbolType() == SymbolType.CONSTRUCTOR) {
                String nodeId = chunk.getClassName() + "." + chunk.getMethodName();
                graph.addNode(RepositoryNode.builder()
                        .id(nodeId)
                        .simpleName(chunk.getMethodName())
                        .type(NodeType.from(chunk.getSymbolType()))
                        .filePath(chunk.getFilePath())
                        .packageName(chunk.getPackageName())
                        .repository(repository)
                        .build());
            }

            if (chunk.getContent() == null) continue;

            if (isTypeDeclaration(chunk.getSymbolType())) {
                extractTypeEdges(graph, chunk);
            } else if (chunk.getSymbolType() == SymbolType.METHOD) {
                extractMethodEdges(graph, chunk);
            }
        }

        log.info("Repository graph built — repository: '{}', nodes: {}", repository, graph.nodeCount());
        return graph;
    }

    private void extractTypeEdges(RepositoryGraph graph, CodeChunkEntity chunk) {
        String content = chunk.getContent();
        String className = chunk.getClassName();

        // EXTENDS edges
        Matcher extendsMatcher = EXTENDS_PATTERN.matcher(content);
        if (extendsMatcher.find()) {
            String parent = extendsMatcher.group(1).trim();
            if (!parent.equals(className)) {
                graph.addEdge(className, parent, EdgeType.EXTENDS);
                log.debug("Edge: {} --EXTENDS--> {}", className, parent);
            }
        }

        // IMPLEMENTS edges
        Matcher implMatcher = IMPLEMENTS_PATTERN.matcher(content);
        if (implMatcher.find()) {
            String[] ifaces = implMatcher.group(1).split(",");
            for (String iface : ifaces) {
                String trimmed = iface.trim();
                if (!trimmed.isEmpty() && !trimmed.equals(className)) {
                    graph.addEdge(className, trimmed, EdgeType.IMPLEMENTS);
                    log.debug("Edge: {} --IMPLEMENTS--> {}", className, trimmed);
                }
            }
        }

        // DEPENDS_ON edges (field injection)
        Matcher fieldMatcher = FIELD_PATTERN.matcher(content);
        while (fieldMatcher.find()) {
            String fieldType = fieldMatcher.group(1).trim();
            if (graph.containsNode(fieldType) && !fieldType.equals(className)) {
                graph.addEdge(className, fieldType, EdgeType.DEPENDS_ON);
                log.debug("Edge: {} --DEPENDS_ON--> {}", className, fieldType);
            }
        }
    }

    private void extractMethodEdges(RepositoryGraph graph, CodeChunkEntity chunk) {
        String content = chunk.getContent();
        String methodId = chunk.getClassName() + "." + chunk.getMethodName();

        // CALLS edges — look for known class names being called
        Matcher callMatcher = METHOD_CALL_PATTERN.matcher(content);
        while (callMatcher.find()) {
            String callee = callMatcher.group(1);
            if (graph.containsNode(callee) && !callee.equals(chunk.getClassName())) {
                graph.addEdge(methodId, callee, EdgeType.CALLS);
            }
        }
    }

    private boolean isTypeDeclaration(SymbolType type) {
        return type == SymbolType.CLASS || type == SymbolType.INTERFACE
                || type == SymbolType.ENUM || type == SymbolType.RECORD;
    }
}
