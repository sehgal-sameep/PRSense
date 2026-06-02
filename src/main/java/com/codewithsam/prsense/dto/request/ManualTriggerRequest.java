package com.codewithsam.prsense.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request body for POST /reviews/trigger — triggerType is set by the controller, not the caller
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualTriggerRequest {

    @NotBlank(message = "repositoryId is required")
    private String repositoryId;

    @Positive(message = "pullRequestId must be a positive integer")
    private int pullRequestId;

    @NotBlank(message = "projectName is required")
    private String projectName;
}
