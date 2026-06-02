package com.codewithsam.prsense.dto.response;

import com.codewithsam.prsense.dto.request.TriggerType;
import com.codewithsam.prsense.model.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatusResponse {

    private String reviewId;
    private ReviewStatus status;
    private TriggerType triggerType;
    private String repositoryId;
    private int pullRequestId;
    private String projectName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalFilesReviewed;
    private Integer totalCommentsGenerated;
    private String failureReason;
}
