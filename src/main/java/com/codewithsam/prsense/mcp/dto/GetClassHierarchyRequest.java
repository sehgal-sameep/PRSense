package com.codewithsam.prsense.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GetClassHierarchyRequest {

    @JsonProperty(value = "className", required = true)
    private String className;

    @JsonProperty(value = "repository", required = true)
    private String repository;
}
