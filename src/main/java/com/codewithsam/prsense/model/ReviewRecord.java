package com.codewithsam.prsense.model;

import com.codewithsam.prsense.dto.request.TriggerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRecord {

    private String reviewId;
    private TriggerType triggerType;
    private String repositoryId;
    private int pullRequestId;
    private String projectName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReviewStatus status;
    private int totalFilesReviewed;
    private int totalCommentsGenerated;
    private String failureReason;
}
