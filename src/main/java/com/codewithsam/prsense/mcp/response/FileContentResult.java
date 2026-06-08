package com.codewithsam.prsense.mcp.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FileContentResult {

    private String path;
    private String content;
    private int totalLines;
    private Integer startLine;
    private Integer endLine;
    private boolean truncated;
}
