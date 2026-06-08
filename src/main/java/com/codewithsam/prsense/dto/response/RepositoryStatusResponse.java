package com.codewithsam.prsense.dto.response;

import com.codewithsam.prsense.entity.RepositoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RepositoryStatusResponse {

    private String repositoryId;
    private String projectName;
    private String repositoryName;
    private String defaultBranch;
    private RepositoryStatus status;
    private String lastIndexedCommit;
    private LocalDateTime lastIndexedAt;
    private int chunkCount;
    private int fileCount;
    private int indexVersion;
    private String failureReason;
}
