package com.codewithsam.prsense.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("vector")
@Getter
@Setter
public class VectorProperties {

    private int searchLimit = 10;
    private int dimensions = 1536;
}
