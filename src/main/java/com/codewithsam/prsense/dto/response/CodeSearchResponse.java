package com.codewithsam.prsense.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSearchResponse {

    private String query;
    private int resultCount;
    private List<CodeChunkDto> results;
}
