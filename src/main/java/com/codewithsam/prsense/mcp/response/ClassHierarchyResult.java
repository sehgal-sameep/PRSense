package com.codewithsam.prsense.mcp.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClassHierarchyResult {

    private String className;
    private List<String> parentClasses;
    private List<String> implementedInterfaces;
    private List<String> childClasses;
    private List<String> childInterfaces;
}
