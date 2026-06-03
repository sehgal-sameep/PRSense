package com.codewithsam.prsense.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("indexing")
@Getter
@Setter
public class IndexingProperties {

    private int chunkBatchSize = 10;
    private long maxFileSizeBytes = 100 * 1024L; // 100 KB
    private List<String> supportedExtensions = List.of(".java");
}
