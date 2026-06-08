package com.codewithsam.prsense.mcp.service;

import com.codewithsam.prsense.mcp.dto.FindReferencesRequest;
import com.codewithsam.prsense.mcp.dto.FindImplementationsRequest;
import com.codewithsam.prsense.mcp.response.ImplementationResult;
import com.codewithsam.prsense.mcp.response.ReferenceResult;

public interface ReferenceSearchService {

    ReferenceResult findReferences(FindReferencesRequest request);

    ImplementationResult findImplementations(FindImplementationsRequest request);
}
