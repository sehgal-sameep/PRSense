package com.codewithsam.prsense.mcp.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewHistoryResult {

    private String query;
    private int totalResults;
    private List<HistoricalReview> reviews;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HistoricalReview {
        private String reviewId;
        private String repositoryId;
        private int pullRequestId;
        private String triggerType;
        private String status;
        private int totalFilesReviewed;
        private int totalCommentsGenerated;
        private LocalDateTime startTime;
    }
}
