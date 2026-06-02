package com.codewithsam.prsense.service;

import com.codewithsam.prsense.model.ReviewRecord;
import com.codewithsam.prsense.model.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Stores and retrieves review execution records for audit and status tracking
public interface ReviewHistoryService {

    void save(ReviewRecord record);

    void update(ReviewRecord record);

    Optional<ReviewRecord> findById(String reviewId);

    List<ReviewRecord> findAll(String repositoryId, Integer pullRequestId,
                               LocalDateTime from, LocalDateTime to, ReviewStatus status);
}
