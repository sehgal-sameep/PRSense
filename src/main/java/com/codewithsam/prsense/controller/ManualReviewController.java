package com.codewithsam.prsense.controller;

import com.codewithsam.prsense.dto.request.ManualTriggerRequest;
import com.codewithsam.prsense.dto.request.ReviewRequest;
import com.codewithsam.prsense.dto.request.TriggerType;
import com.codewithsam.prsense.dto.response.ReviewStatusResponse;
import com.codewithsam.prsense.manager.ReviewManager;
import com.codewithsam.prsense.model.ReviewRecord;
import com.codewithsam.prsense.model.ReviewStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Review APIs", description = "APIs for triggering and monitoring PR reviews")
public class ManualReviewController {

    private final ReviewManager reviewManager;

    @PostMapping("/trigger")
    @Operation(
            summary = "Manually trigger a PR review",
            description = "Queues a PR review and returns immediately with a reviewId for status tracking"
    )
    public ResponseEntity<ReviewStatusResponse> triggerReview(
            @RequestBody @Valid ManualTriggerRequest request) {

        ReviewRequest reviewRequest = ReviewRequest.builder()
                .triggerType(TriggerType.MANUAL)
                .repositoryId(request.getRepositoryId())
                .pullRequestId(request.getPullRequestId())
                .projectName(request.getProjectName())
                .build();

        String reviewId = reviewManager.processReview(reviewRequest);
        log.info("Manual review triggered: {} — PR #{}", reviewId, request.getPullRequestId());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ReviewStatusResponse.builder()
                        .reviewId(reviewId)
                        .status(ReviewStatus.QUEUED)
                        .triggerType(TriggerType.MANUAL)
                        .repositoryId(request.getRepositoryId())
                        .pullRequestId(request.getPullRequestId())
                        .projectName(request.getProjectName())
                        .build());
    }

    @GetMapping("/{reviewId}")
    @Operation(
            summary = "Get review status",
            description = "Returns the current status and execution details for a review job"
    )
    public ResponseEntity<ReviewStatusResponse> getReviewStatus(@PathVariable String reviewId) {
        try {
            return ResponseEntity.ok(toResponse(reviewManager.getReview(reviewId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/history")
    @Operation(
            summary = "Get review history",
            description = "Lists review execution history with optional filters by repository, PR, date range, or status"
    )
    public ResponseEntity<List<ReviewStatusResponse>> getHistory(
            @RequestParam(required = false) String repositoryId,
            @RequestParam(required = false) Integer pullRequestId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) ReviewStatus status) {

        List<ReviewStatusResponse> history = reviewManager
                .getReviewHistory(repositoryId, pullRequestId, from, to, status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    private ReviewStatusResponse toResponse(ReviewRecord record) {
        return ReviewStatusResponse.builder()
                .reviewId(record.getReviewId())
                .status(record.getStatus())
                .triggerType(record.getTriggerType())
                .repositoryId(record.getRepositoryId())
                .pullRequestId(record.getPullRequestId())
                .projectName(record.getProjectName())
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .totalFilesReviewed(record.getTotalFilesReviewed())
                .totalCommentsGenerated(record.getTotalCommentsGenerated())
                .failureReason(record.getFailureReason())
                .build();
    }
}
