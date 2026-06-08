package com.codewithsam.prsense.mcp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("prsense.mcp")
@Getter
@Setter
public class McpProperties {

    private boolean enabled = true;
    private int defaultSearchLimit = 10;
    private int maxSearchLimit = 50;
    private String serverName = "prsense-mcp-server";
}
