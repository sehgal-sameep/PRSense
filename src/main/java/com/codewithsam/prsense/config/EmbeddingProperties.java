package com.codewithsam.prsense.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("embedding")
@Getter
@Setter
public class EmbeddingProperties {

    private String model = "text-embedding-3-small";
    private int dimensions = 1536;
    private int batchSize = 20;
}
