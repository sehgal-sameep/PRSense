package com.codewithsam.prsense.mcp.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryGraphTest {

    private RepositoryGraph graph;

    @BeforeEach
    void setUp() {
        graph = new RepositoryGraph();
        graph.addNode(node("UserService", NodeType.CLASS));
        graph.addNode(node("IUserService", NodeType.INTERFACE));
        graph.addNode(node("UserRepository", NodeType.CLASS));
        graph.addNode(node("UserController", NodeType.CLASS));
    }

    @Test
    void addAndRetrieveNode() {
        assertThat(graph.getNode("UserService")).isPresent();
        assertThat(graph.getNode("UserService").get().getType()).isEqualTo(NodeType.CLASS);
        assertThat(graph.getNode("NonExistent")).isEmpty();
    }

    @Test
    void outgoing_returnsEdgeTargets() {
        graph.addEdge("UserService", "IUserService", EdgeType.IMPLEMENTS);
        graph.addEdge("UserService", "UserRepository", EdgeType.DEPENDS_ON);

        Set<String> implemented = graph.outgoing("UserService", EdgeType.IMPLEMENTS);
        assertThat(implemented).containsExactly("IUserService");

        Set<String> deps = graph.outgoing("UserService", EdgeType.DEPENDS_ON);
        assertThat(deps).containsExactly("UserRepository");
    }

    @Test
    void incoming_returnsReverseEdges() {
        graph.addEdge("UserService", "IUserService", EdgeType.IMPLEMENTS);
        graph.addEdge("UserController", "UserService", EdgeType.DEPENDS_ON);

        Set<String> implementors = graph.incoming("IUserService", EdgeType.IMPLEMENTS);
        assertThat(implementors).containsExactly("UserService");

        Set<String> dependents = graph.incoming("UserService", EdgeType.DEPENDS_ON);
        assertThat(dependents).containsExactly("UserController");
    }

    @Test
    void nodesOfType_filtersCorrectly() {
        assertThat(graph.nodesOfType(NodeType.INTERFACE)).hasSize(1);
        assertThat(graph.nodesOfType(NodeType.CLASS)).hasSize(3);
    }

    @Test
    void nodeCount_returnsCorrectTotal() {
        assertThat(graph.nodeCount()).isEqualTo(4);
    }

    @Test
    void outgoing_returnsEmptyForUnknownNode() {
        assertThat(graph.outgoing("Unknown", EdgeType.EXTENDS)).isEmpty();
    }

    private RepositoryNode node(String name, NodeType type) {
        return RepositoryNode.builder()
                .id(name).simpleName(name).type(type)
                .filePath("src/" + name + ".java").repository("test-repo")
                .build();
    }
}
