package com.codewithsam.prsense.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexStatusResponse {

    private String repository;
    private long totalChunks;
    private long totalFiles;
    private Map<String, Long> chunksBySymbolType;
    private LocalDateTime lastIndexedAt;
}
