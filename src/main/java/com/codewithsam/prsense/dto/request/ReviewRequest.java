package com.codewithsam.prsense.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Internal DTO representing a review job regardless of how it was initiated
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    private TriggerType triggerType;
    private String repositoryId;
    private int pullRequestId;
    private String projectName;
}
