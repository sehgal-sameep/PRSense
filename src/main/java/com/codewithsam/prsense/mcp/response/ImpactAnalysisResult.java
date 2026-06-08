package com.codewithsam.prsense.mcp.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ImpactAnalysisResult {

    private String className;
    private List<String> affectedControllers;
    private List<String> affectedServices;
    private List<String> affectedRepositories;
    private List<String> affectedScheduledJobs;
    private List<String> affectedEventListeners;
    private int totalAffectedComponents;
}
