package com.codewithsam.prsense.service.impl;

import com.codewithsam.prsense.model.ReviewRecord;
import com.codewithsam.prsense.model.ReviewStatus;
import com.codewithsam.prsense.service.ReviewHistoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ReviewHistoryServiceImpl implements ReviewHistoryService {

    private final Map<String, ReviewRecord> store = new ConcurrentHashMap<>();

    @Override
    public void save(ReviewRecord record) {
        store.put(record.getReviewId(), record);
    }

    @Override
    public void update(ReviewRecord record) {
        store.put(record.getReviewId(), record);
    }

    @Override
    public Optional<ReviewRecord> findById(String reviewId) {
        return Optional.ofNullable(store.get(reviewId));
    }

    @Override
    public List<ReviewRecord> findAll(String repositoryId, Integer pullRequestId,
                                      LocalDateTime from, LocalDateTime to, ReviewStatus status) {
        return store.values().stream()
                .filter(r -> repositoryId == null || repositoryId.equals(r.getRepositoryId()))
                .filter(r -> pullRequestId == null || pullRequestId == r.getPullRequestId())
                .filter(r -> from == null || (r.getStartTime() != null && !r.getStartTime().isBefore(from)))
                .filter(r -> to == null || (r.getStartTime() != null && !r.getStartTime().isAfter(to)))
                .filter(r -> status == null || status == r.getStatus())
                .collect(Collectors.toList());
    }
}
