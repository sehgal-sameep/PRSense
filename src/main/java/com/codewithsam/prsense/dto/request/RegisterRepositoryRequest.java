package com.codewithsam.prsense.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegisterRepositoryRequest {

    @NotBlank(message = "projectName is required")
    private String projectName;

    @NotBlank(message = "repositoryName is required")
    private String repositoryName;

    // Optional: override the detected default branch
    private String branch;
}
