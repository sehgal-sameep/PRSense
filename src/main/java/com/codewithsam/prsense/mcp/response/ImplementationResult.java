package com.codewithsam.prsense.mcp.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ImplementationResult {

    private String interfaceName;
    private int totalImplementations;
    private List<Implementation> implementations;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Implementation {
        private String className;
        private String filePath;
        private String packageName;
    }
}
