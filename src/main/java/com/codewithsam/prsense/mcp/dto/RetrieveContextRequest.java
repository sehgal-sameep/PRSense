package com.codewithsam.prsense.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RetrieveContextRequest {

    @JsonProperty(value = "pullRequestId", required = true)
    private int pullRequestId;

    @JsonProperty(value = "repository", required = true)
    private String repository;

    @JsonProperty("changedFiles")
    private java.util.List<String> changedFiles;
}
