package com.codewithsam.prsense.mcp.graph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// In-memory directed graph of repository symbols and their relationships.
// Nodes = classes/interfaces/methods; Edges = EXTENDS, IMPLEMENTS, DEPENDS_ON, etc.
public class RepositoryGraph {

    private final Map<String, RepositoryNode> nodes = new ConcurrentHashMap<>();

    // edgeType → fromId → Set<toId>
    private final Map<EdgeType, Map<String, Set<String>>> adj = new ConcurrentHashMap<>();

    // edgeType → toId → Set<fromId>  (reverse for incoming lookups)
    private final Map<EdgeType, Map<String, Set<String>>> revAdj = new ConcurrentHashMap<>();

    public void addNode(RepositoryNode node) {
        nodes.put(node.getId(), node);
    }

    public void addEdge(String fromId, String toId, EdgeType type) {
        adj.computeIfAbsent(type, k -> new ConcurrentHashMap<>())
           .computeIfAbsent(fromId, k -> ConcurrentHashMap.newKeySet())
           .add(toId);
        revAdj.computeIfAbsent(type, k -> new ConcurrentHashMap<>())
              .computeIfAbsent(toId, k -> ConcurrentHashMap.newKeySet())
              .add(fromId);
    }

    // Returns the IDs of nodes that fromId points TO via edgeType
    public Set<String> outgoing(String fromId, EdgeType type) {
        return adj.getOrDefault(type, Map.of()).getOrDefault(fromId, Set.of());
    }

    // Returns the IDs of nodes that point TO toId via edgeType
    public Set<String> incoming(String toId, EdgeType type) {
        return revAdj.getOrDefault(type, Map.of()).getOrDefault(toId, Set.of());
    }

    public Optional<RepositoryNode> getNode(String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public Collection<RepositoryNode> allNodes() {
        return nodes.values();
    }

    public Collection<RepositoryNode> nodesOfType(NodeType type) {
        return nodes.values().stream().filter(n -> n.getType() == type).toList();
    }

    public boolean containsNode(String id) {
        return nodes.containsKey(id);
    }

    public int nodeCount() {
        return nodes.size();
    }
}
