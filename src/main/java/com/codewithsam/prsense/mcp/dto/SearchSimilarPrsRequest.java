package com.codewithsam.prsense.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SearchSimilarPrsRequest {

    @JsonProperty(value = "query", required = true)
    private String query;

    @JsonProperty("repositoryId")
    private String repositoryId;

    @JsonProperty("limit")
    private Integer limit;
}
