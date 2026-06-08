package com.codewithsam.prsense.mcp.service.impl;

import com.codewithsam.prsense.mcp.service.ToolMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolMetricsServiceImpl implements ToolMetricsService {

    private final MeterRegistry meterRegistry;

    @Override
    public void recordToolStart(String toolName, String contextId) {
        log.debug("Tool execution started — tool: {}, context: {}", toolName, contextId);
        Counter.builder("mcp.tool.invocations.started")
                .tag("tool", toolName)
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void recordToolSuccess(String toolName, String contextId, long durationMs) {
        log.info("Tool execution completed — tool: {}, context: {}, duration: {}ms",
                toolName, contextId, durationMs);
        Timer.builder("mcp.tool.execution.duration")
                .tag("tool", toolName)
                .tag("status", "success")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        Counter.builder("mcp.tool.invocations.total")
                .tag("tool", toolName)
                .tag("status", "success")
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void recordToolFailure(String toolName, String contextId, String errorType) {
        log.warn("Tool failure occurred — tool: {}, context: {}, error: {}",
                toolName, contextId, errorType);
        Counter.builder("mcp.tool.invocations.total")
                .tag("tool", toolName)
                .tag("status", "failure")
                .tag("error", errorType)
                .register(meterRegistry)
                .increment();
    }
}
