package com.codewithsam.prsense.mcp.service;

import com.codewithsam.prsense.mcp.dto.SearchCodeRequest;
import com.codewithsam.prsense.mcp.response.CodeSearchResult;

public interface CodeSearchToolService {

    CodeSearchResult search(SearchCodeRequest request);
}
