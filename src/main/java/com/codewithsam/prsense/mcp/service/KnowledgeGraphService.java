package com.codewithsam.prsense.mcp.service;

import com.codewithsam.prsense.mcp.dto.AnalyzeImpactRequest;
import com.codewithsam.prsense.mcp.dto.GetClassHierarchyRequest;
import com.codewithsam.prsense.mcp.dto.GetRelatedCodeRequest;
import com.codewithsam.prsense.mcp.graph.RepositoryGraph;
import com.codewithsam.prsense.mcp.response.ClassHierarchyResult;
import com.codewithsam.prsense.mcp.response.ImpactAnalysisResult;
import com.codewithsam.prsense.mcp.response.RelatedCodeResult;

public interface KnowledgeGraphService {

    RepositoryGraph getOrBuildGraph(String repository);

    void invalidateGraph(String repository);

    ClassHierarchyResult getClassHierarchy(GetClassHierarchyRequest request);

    RelatedCodeResult getRelatedCode(GetRelatedCodeRequest request);

    ImpactAnalysisResult analyzeImpact(AnalyzeImpactRequest request);
}
