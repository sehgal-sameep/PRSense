package com.codewithsam.prsense.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("azure.devops")
@Validated
@Getter
@Setter
public class AzureDevOpsProperties {

    @NotBlank
    private String baseUrl;

    // Personal access token — never log this
    @NotBlank
    private String pat;

    private String org;
}
