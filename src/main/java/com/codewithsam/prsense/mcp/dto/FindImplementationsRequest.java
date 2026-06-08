package com.codewithsam.prsense.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FindImplementationsRequest {

    @JsonProperty(value = "interfaceName", required = true)
    private String interfaceName;

    @JsonProperty(value = "repository", required = true)
    private String repository;
}
