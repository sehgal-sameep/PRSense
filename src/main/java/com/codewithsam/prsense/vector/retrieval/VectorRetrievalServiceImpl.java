package com.codewithsam.prsense.vector.retrieval;

import com.codewithsam.prsense.dto.response.CodeChunkDto;
import com.codewithsam.prsense.exception.VectorSearchException;
import com.codewithsam.prsense.mapper.CodeChunkMapper;
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
public class VectorRetrievalServiceImpl implements VectorRetrievalService {

    private final EmbeddingService embeddingService;
    private final CodeChunkRepository codeChunkRepository;
    private final CodeChunkMapper codeChunkMapper;

    @Override
    public List<CodeChunkDto> search(String repository, String query, int limit) {
        log.info("Vector search — repository: '{}', query: '{}', limit: {}", repository, query, limit);

        try {
            float[] queryEmbedding = embeddingService.embed(query);
            String embeddingStr = VectorUtil.toVectorString(queryEmbedding);

            List<CodeChunkDto> results = codeChunkMapper.toDtoList(
                    codeChunkRepository.findTopNSimilar(repository, embeddingStr, limit));

            log.info("Vector search returned {} result(s) for repository '{}'", results.size(), repository);
            return results;

        } catch (Exception e) {
            log.error("Vector search failed for repository '{}': {}", repository, e.getMessage(), e);
            throw new VectorSearchException("Vector search failed: " + e.getMessage(), e);
        }
    }
}
