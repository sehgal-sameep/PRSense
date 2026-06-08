package com.codewithsam.prsense.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GetFileRequest {

    @JsonProperty(value = "path", required = true)
    private String path;

    @JsonProperty(value = "repository", required = true)
    private String repository;

    @JsonProperty("startLine")
    private Integer startLine;

    @JsonProperty("endLine")
    private Integer endLine;
}
