package com.codewithsam.prsense.mcp.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RepositoryNode {

    // Unique ID within a repository: "ClassName" or "ClassName.methodName"
    private String id;
    private String simpleName;
    private NodeType type;
    private String filePath;
    private String packageName;
    private String repository;
}
