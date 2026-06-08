package com.codewithsam.prsense.mcp.service;

import com.codewithsam.prsense.mcp.dto.RetrieveContextRequest;
import com.codewithsam.prsense.mcp.response.ContextRetrievalResult;

public interface McpContextRetrievalService {

    ContextRetrievalResult retrieveContext(RetrieveContextRequest request);
}
