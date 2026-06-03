package com.codewithsam.prsense.manager.impl;

import com.codewithsam.prsense.config.VectorProperties;
import com.codewithsam.prsense.dto.response.CodeChunkDto;
import com.codewithsam.prsense.dto.response.CodeSearchResponse;
import com.codewithsam.prsense.manager.CodeSearchManager;
import com.codewithsam.prsense.vector.retrieval.VectorRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeSearchManagerImpl implements CodeSearchManager {

    private final VectorRetrievalService vectorRetrievalService;
    private final VectorProperties vectorProperties;

    @Override
    public CodeSearchResponse search(String query, String repository, int limit) {
        int effectiveLimit = limit > 0 ? limit : vectorProperties.getSearchLimit();
        log.info("Code search — repository: '{}', limit: {}", repository, effectiveLimit);

        List<CodeChunkDto> results = vectorRetrievalService.search(repository, query, effectiveLimit);

        return CodeSearchResponse.builder()
                .query(query)
                .resultCount(results.size())
                .results(results)
                .build();
    }
}
