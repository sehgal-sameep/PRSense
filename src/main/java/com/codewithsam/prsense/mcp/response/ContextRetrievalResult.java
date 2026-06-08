package com.codewithsam.prsense.mcp.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ContextRetrievalResult {

    private int pullRequestId;
    private String repository;
    private List<CodeSearchResult.CodeMatch> relevantCode;
    private List<ReferenceResult.Reference> keyReferences;
    private List<ReviewHistoryResult.HistoricalReview> historicalReviews;
    private List<String> relatedClasses;
    private String aggregatedContext;
}
