package com.codewithsam.prsense.mcp.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CodeSearchResult {

    private String query;
    private int totalResults;
    private List<CodeMatch> matches;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CodeMatch {
        private String filePath;
        private String className;
        private String methodName;
        private String symbolType;
        private String contentSnippet;
    }
}
