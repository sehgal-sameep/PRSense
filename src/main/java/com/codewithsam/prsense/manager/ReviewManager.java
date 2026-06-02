package com.codewithsam.prsense.manager;

import com.codewithsam.prsense.dto.request.ReviewRequest;
import com.codewithsam.prsense.model.ReviewRecord;
import com.codewithsam.prsense.model.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;

// Single orchestration entry point for all review triggers — webhook, manual, scheduled, etc.
// Controllers create a ReviewRequest and call processReview(); this interface owns all review flow.
public interface ReviewManager {

    // Queues the review for async execution and returns a reviewId immediately
    String processReview(ReviewRequest request);

    ReviewRecord getReview(String reviewId);

    List<ReviewRecord> getReviewHistory(String repositoryId, Integer pullRequestId,
                                        LocalDateTime from, LocalDateTime to, ReviewStatus status);
}
