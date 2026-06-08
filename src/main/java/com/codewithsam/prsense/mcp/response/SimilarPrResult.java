package com.codewithsam.prsense.mcp.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SimilarPrResult {

    private String query;
    private int totalResults;
    private List<SimilarPr> pullRequests;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SimilarPr {
        private String reviewId;
        private int pullRequestId;
        private String repositoryId;
        private String status;
        private int totalComments;
    }
}
