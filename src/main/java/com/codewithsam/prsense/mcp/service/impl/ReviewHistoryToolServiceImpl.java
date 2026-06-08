package com.codewithsam.prsense.mcp.service.impl;

import com.codewithsam.prsense.mcp.config.McpProperties;
import com.codewithsam.prsense.mcp.dto.SearchReviewHistoryRequest;
import com.codewithsam.prsense.mcp.dto.SearchSimilarPrsRequest;
import com.codewithsam.prsense.mcp.response.ReviewHistoryResult;
import com.codewithsam.prsense.mcp.response.SimilarPrResult;
import com.codewithsam.prsense.mcp.service.ReviewHistoryToolService;
import com.codewithsam.prsense.model.ReviewRecord;
import com.codewithsam.prsense.service.ReviewHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewHistoryToolServiceImpl implements ReviewHistoryToolService {

    private final ReviewHistoryService reviewHistoryService;
    private final McpProperties mcpProperties;

    @Override
    public ReviewHistoryResult searchHistory(SearchReviewHistoryRequest request) {
        log.info("Historical review search — query: '{}', repositoryId: '{}'",
                request.getQuery(), request.getRepositoryId());

        int limit = request.getLimit() != null ? request.getLimit() : mcpProperties.getDefaultSearchLimit();
        String queryLower = request.getQuery().toLowerCase();

        List<ReviewRecord> all = reviewHistoryService.findAll(
                request.getRepositoryId(), null, null, null, null);

        List<ReviewHistoryResult.HistoricalReview> matched = all.stream()
                .filter(r -> r.getRepositoryId() != null
                        && r.getRepositoryId().toLowerCase().contains(queryLower))
                .limit(limit)
                .map(r -> ReviewHistoryResult.HistoricalReview.builder()
                        .reviewId(r.getReviewId())
                        .repositoryId(r.getRepositoryId())
                        .pullRequestId(r.getPullRequestId())
                        .triggerType(r.getTriggerType() != null ? r.getTriggerType().name() : null)
                        .status(r.getStatus() != null ? r.getStatus().name() : null)
                        .totalFilesReviewed(r.getTotalFilesReviewed())
                        .totalCommentsGenerated(r.getTotalCommentsGenerated())
                        .startTime(r.getStartTime())
                        .build())
                .toList();

        log.info("Historical review search completed — results: {}", matched.size());

        return ReviewHistoryResult.builder()
                .query(request.getQuery())
                .totalResults(matched.size())
                .reviews(matched)
                .build();
    }

    @Override
    public SimilarPrResult searchSimilarPrs(SearchSimilarPrsRequest request) {
        log.info("Similar PR search — query: '{}', repositoryId: '{}'",
                request.getQuery(), request.getRepositoryId());

        int limit = request.getLimit() != null ? request.getLimit() : mcpProperties.getDefaultSearchLimit();

        List<ReviewRecord> all = reviewHistoryService.findAll(
                request.getRepositoryId(), null, null, null, null);

        List<SimilarPrResult.SimilarPr> prs = all.stream()
                .limit(limit)
                .map(r -> SimilarPrResult.SimilarPr.builder()
                        .reviewId(r.getReviewId())
                        .pullRequestId(r.getPullRequestId())
                        .repositoryId(r.getRepositoryId())
                        .status(r.getStatus() != null ? r.getStatus().name() : null)
                        .totalComments(r.getTotalCommentsGenerated())
                        .build())
                .toList();

        log.info("Similar PR search completed — results: {}", prs.size());

        return SimilarPrResult.builder()
                .query(request.getQuery())
                .totalResults(prs.size())
                .pullRequests(prs)
                .build();
    }
}
