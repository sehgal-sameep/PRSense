package com.codewithsam.prsense.mcp.service.impl;

import com.codewithsam.prsense.mcp.config.McpProperties;
import com.codewithsam.prsense.mcp.dto.SearchCodeRequest;
import com.codewithsam.prsense.mcp.response.CodeSearchResult;
import com.codewithsam.prsense.mcp.service.CodeSearchToolService;
import com.codewithsam.prsense.repository.CodeChunkRepository;
import com.codewithsam.prsense.util.VectorUtil;
import com.codewithsam.prsense.vector.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeSearchToolServiceImpl implements CodeSearchToolService {

    private final EmbeddingService embeddingService;
    private final CodeChunkRepository codeChunkRepository;
    private final McpProperties mcpProperties;

    @Override
    public CodeSearchResult search(SearchCodeRequest request) {
        int limit = request.getLimit() != null
                ? Math.min(request.getLimit(), mcpProperties.getMaxSearchLimit())
                : mcpProperties.getDefaultSearchLimit();

        log.info("Vector search executed — repository: '{}', query: '{}', limit: {}",
                request.getRepository(), request.getQuery(), limit);

        float[] embedding = embeddingService.embed(request.getQuery());
        String embeddingStr = VectorUtil.toVectorString(embedding);

        List<CodeSearchResult.CodeMatch> matches = codeChunkRepository
                .findTopNSimilar(request.getRepository(), embeddingStr, limit)
                .stream()
                .map(chunk -> CodeSearchResult.CodeMatch.builder()
                        .filePath(chunk.getFilePath())
                        .className(chunk.getClassName())
                        .methodName(chunk.getMethodName())
                        .symbolType(chunk.getSymbolType() != null ? chunk.getSymbolType().name() : null)
                        .contentSnippet(snippet(chunk.getContent()))
                        .build())
                .toList();

        if (matches.isEmpty()) {
            log.warn("Vector search returned 0 results -- repository: '{}'. Repository may not be indexed. Run POST /index/repository with repositoryName='{}'.", request.getRepository(), request.getRepository());
        } else {
            log.info("Vector search returned {} result(s) -- repository: '{}'", matches.size(), request.getRepository());
        }

        return CodeSearchResult.builder()
                .query(request.getQuery())
                .totalResults(matches.size())
                .matches(matches)
                .build();
    }

    private String snippet(String content) {
        if (content == null) return "";
        return content.length() > 300 ? content.substring(0, 300) + "…" : content;
    }
}
