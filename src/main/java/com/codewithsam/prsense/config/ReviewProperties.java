package com.codewithsam.prsense.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("review")
@Getter
@Setter
public class ReviewProperties {

    private int maxFiles = 15;
    private int maxLines = 300;
    private List<String> skipExtensions;
    private int threadPoolSize = 5;
}
