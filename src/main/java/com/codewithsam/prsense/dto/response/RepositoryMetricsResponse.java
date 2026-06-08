package com.codewithsam.prsense.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RepositoryMetricsResponse {

    private String repositoryId;
    private String repositoryName;
    private int totalChunks;
    private int totalFiles;
    private int indexVersion;
    private long totalIndexDurationMs;
}
