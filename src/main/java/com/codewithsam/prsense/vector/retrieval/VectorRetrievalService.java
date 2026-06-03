package com.codewithsam.prsense.vector.retrieval;

import com.codewithsam.prsense.dto.response.CodeChunkDto;

import java.util.List;

// Performs semantic similarity search over indexed code chunks
public interface VectorRetrievalService {

    List<CodeChunkDto> search(String repository, String query, int limit);
}
