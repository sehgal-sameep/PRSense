package com.codewithsam.prsense.mcp.service;

public interface ToolMetricsService {

    void recordToolStart(String toolName, String contextId);

    void recordToolSuccess(String toolName, String contextId, long durationMs);

    void recordToolFailure(String toolName, String contextId, String errorType);
}
