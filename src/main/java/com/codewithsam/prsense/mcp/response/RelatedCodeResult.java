package com.codewithsam.prsense.mcp.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RelatedCodeResult {

    private String className;
    private List<RelatedComponent> services;
    private List<RelatedComponent> repositories;
    private List<RelatedComponent> controllers;
    private List<RelatedComponent> dtos;
    private List<RelatedComponent> entities;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RelatedComponent {
        private String className;
        private String filePath;
        private String symbolType;
    }
}
