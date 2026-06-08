package com.codewithsam.prsense.mcp.service;

import com.codewithsam.prsense.mcp.dto.SearchReviewHistoryRequest;
import com.codewithsam.prsense.mcp.dto.SearchSimilarPrsRequest;
import com.codewithsam.prsense.mcp.response.ReviewHistoryResult;
import com.codewithsam.prsense.mcp.response.SimilarPrResult;

public interface ReviewHistoryToolService {

    ReviewHistoryResult searchHistory(SearchReviewHistoryRequest request);

    SimilarPrResult searchSimilarPrs(SearchSimilarPrsRequest request);
}
