package com.codewithsam.prsense.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SearchCodeRequest {

    @JsonProperty(value = "query", required = true)
    private String query;

    @JsonProperty(value = "repository", required = true)
    private String repository;

    @JsonProperty("limit")
    private Integer limit;
}
