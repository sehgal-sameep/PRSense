package com.codewithsam.prsense.mcp.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReferenceResult {

    private String symbol;
    private int totalReferences;
    private List<Reference> references;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Reference {
        private String filePath;
        private String className;
        private String methodName;
        private String symbolType;
    }
}
