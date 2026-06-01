package com.codewithsam.prsense.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("openai")
@Validated
@Getter
@Setter
public class OpenAiProperties {

    // API key — never log this
    @NotBlank
    private String apiKey;

    @NotBlank
    private String model;

    private int maxTokens = 4096;

    private double temperature = 0.3;
}
