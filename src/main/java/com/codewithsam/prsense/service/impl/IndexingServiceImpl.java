package com.codewithsam.prsense.service.impl;

import com.codewithsam.prsense.config.IndexingProperties;
import com.codewithsam.prsense.dto.response.IndexStatusResponse;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import com.codewithsam.prsense.exception.RepositoryIndexingException;
import com.codewithsam.prsense.parser.CodeParser;
import com.codewithsam.prsense.parser.ParsedSymbol;
import com.codewithsam.prsense.repository.CodeChunkRepository;
import com.codewithsam.prsense.service.IndexingService;
import com.codewithsam.prsense.util.VectorUtil;
import com.codewithsam.prsense.vector.embedding.EmbeddingService;
import com.codewithsam.prsense.vector.indexing.CodeChunker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final List<CodeParser> codeParsers;
    private final CodeChunker codeChunker;
    private final EmbeddingService embeddingService;
    private final CodeChunkRepository codeChunkRepository;
    private final IndexingProperties indexingProperties;

    @Override
    @Transactional
    public void indexRepository(String repositoryName, String repositoryPath) {
        log.info("Repository indexing started — repository: '{}', path: '{}'", repositoryName, repositoryPath);

        Path root = Paths.get(repositoryPath);
        if (!Files.isDirectory(root)) {
            throw new RepositoryIndexingException(
                    "Repository path does not exist or is not a directory: " + repositoryPath);
        }

        List<Path> sourceFiles = discoverSourceFiles(root);
        log.info("Discovered {} source file(s) in '{}'", sourceFiles.size(), repositoryPath);

        int filesProcessed = 0;
        int chunksIndexed = 0;
        int chunksSkipped = 0;

        for (Path file : sourceFiles) {
            try {
                IndexResult result = indexFile(repositoryName, file);
                filesProcessed++;
                chunksIndexed += result.indexed();
                chunksSkipped += result.skipped();
            } catch (Exception e) {
                log.warn("Skipping file '{}': {}", file, e.getMessage());
            }
        }

        log.info("Repository indexing completed — repository: '{}', files: {}, chunks indexed: {}, chunks skipped: {}",
                repositoryName, filesProcessed, chunksIndexed, chunksSkipped);
    }

    @Override
    public IndexStatusResponse getIndexStatus(String repositoryName) {
        long totalChunks = codeChunkRepository.countByRepository(repositoryName);
        long totalFiles = codeChunkRepository.findDistinctFilePathsByRepository(repositoryName).size();

        Map<String, Long> chunksByType = new HashMap<>();
        codeChunkRepository.countBySymbolTypeForRepository(repositoryName)
                .forEach(row -> chunksByType.put((String) row[0], (Long) row[1]));

        LocalDateTime lastIndexedAt = codeChunkRepository
                .findTopByRepositoryOrderByCreatedAtDesc(repositoryName)
                .map(CodeChunkEntity::getCreatedAt)
                .orElse(null);

        return IndexStatusResponse.builder()
                .repository(repositoryName)
                .totalChunks(totalChunks)
                .totalFiles(totalFiles)
                .chunksBySymbolType(chunksByType)
                .lastIndexedAt(lastIndexedAt)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    private List<Path> discoverSourceFiles(Path root) {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> isSupported(p.toString()))
                .filter(p -> {
                    try {
                        return Files.size(p) <= indexingProperties.getMaxFileSizeBytes();
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(files::add);
        } catch (IOException e) {
            throw new RepositoryIndexingException("Failed to scan repository: " + e.getMessage(), e);
        }
        return files;
    }

    @Transactional
    protected IndexResult indexFile(String repositoryName, Path file) throws IOException {
        String source = Files.readString(file);
        String filePath = file.toString();

        CodeParser parser = codeParsers.stream()
                .filter(p -> p.supports(filePath))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No parser for: " + filePath));

        List<ParsedSymbol> symbols = parser.parse(filePath, source);
        if (symbols.isEmpty()) {
            log.debug("No symbols found in '{}' — skipping", filePath);
            return new IndexResult(0, 0);
        }

        List<CodeChunkEntity> candidates = codeChunker.chunk(repositoryName, symbols);

        // Incremental update: only reindex chunks whose content has changed
        List<CodeChunkEntity> toIndex = new ArrayList<>();
        int skipped = 0;
        for (CodeChunkEntity candidate : candidates) {
            if (codeChunkRepository.existsByRepositoryAndFilePathAndContentHash(
                    repositoryName, filePath, candidate.getContentHash())) {
                skipped++;
            } else {
                toIndex.add(candidate);
            }
        }

        if (toIndex.isEmpty()) {
            log.debug("File '{}' unchanged — {} chunk(s) skipped", filePath, skipped);
            return new IndexResult(0, skipped);
        }

        // Remove stale chunks for this file before writing fresh ones
        codeChunkRepository.deleteByRepositoryAndFilePath(repositoryName, filePath);

        List<String> contents = toIndex.stream().map(CodeChunkEntity::getContent).toList();
        List<float[]> embeddings = embeddingService.embedBatch(contents);

        for (int i = 0; i < toIndex.size(); i++) {
            toIndex.get(i).setEmbedding(VectorUtil.toVectorString(embeddings.get(i)));
        }

        codeChunkRepository.saveAll(toIndex);
        log.debug("Indexed {} chunk(s) from '{}' ({} skipped)", toIndex.size(), filePath, skipped);

        return new IndexResult(toIndex.size(), skipped);
    }

    private boolean isSupported(String filePath) {
        return indexingProperties.getSupportedExtensions().stream().anyMatch(filePath::endsWith);
    }

    private record IndexResult(int indexed, int skipped) {}
}
