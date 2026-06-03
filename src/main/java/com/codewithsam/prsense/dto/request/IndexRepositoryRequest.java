package com.codewithsam.prsense.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexRepositoryRequest {

    @NotBlank(message = "repositoryName is required")
    private String repositoryName;

    @NotBlank(message = "repositoryPath is required")
    private String repositoryPath; // Absolute local path to the cloned repository

    private String branch; // optional — for informational purposes only
}
