package com.codewithsam.prsense.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("webhook")
@Validated
@Getter
@Setter
public class WebhookProperties {

    // Shared secret validated on every incoming webhook call
    @NotBlank
    private String secret;
}
