package com.codewithsam.prsense.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReindexRepositoryRequest {

    @NotBlank(message = "repositoryId is required")
    private String repositoryId;

    // If true, wipe all existing chunks before re-indexing (full rebuild)
    private boolean fullRebuild = false;
}
