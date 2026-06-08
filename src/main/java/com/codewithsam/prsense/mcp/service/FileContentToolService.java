package com.codewithsam.prsense.mcp.service;

import com.codewithsam.prsense.mcp.dto.GetFileRequest;
import com.codewithsam.prsense.mcp.response.FileContentResult;

public interface FileContentToolService {

    FileContentResult getFileContent(GetFileRequest request);
}
