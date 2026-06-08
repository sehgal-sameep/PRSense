package com.codewithsam.prsense.service.impl;

import com.codewithsam.prsense.config.IndexingProperties;
import com.codewithsam.prsense.entity.RepositoryRegistryEntity;
import com.codewithsam.prsense.entity.RepositoryStatus;
import com.codewithsam.prsense.exception.RepositoryIndexingException;
import com.codewithsam.prsense.model.AzureCommitChange;
import com.codewithsam.prsense.model.AzureRepositoryItem;
import com.codewithsam.prsense.parser.CodeParser;
import com.codewithsam.prsense.parser.ParsedSymbol;
import com.codewithsam.prsense.repository.CodeChunkRepository;
import com.codewithsam.prsense.repository.RepositoryRegistryRepository;
import com.codewithsam.prsense.service.AzureDevOpsRepositoryService;
import com.codewithsam.prsense.service.AzureDevOpsService;
import com.codewithsam.prsense.service.RepositorySyncService;
import com.codewithsam.prsense.util.VectorUtil;
import com.codewithsam.prsense.vector.embedding.EmbeddingService;
import com.codewithsam.prsense.vector.indexing.CodeChunker;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositorySyncServiceImpl implements RepositorySyncService {

    private final AzureDevOpsRepositoryService azureDevOpsRepositoryService;
    private final AzureDevOpsService azureDevOpsService;
    private final List<CodeParser> codeParsers;
    private final CodeChunker codeChunker;
    private final EmbeddingService embeddingService;
    private final CodeChunkRepository codeChunkRepository;
    private final RepositoryRegistryRepository registryRepository;
    private final IndexingProperties indexingProperties;

    @Override
    @Transactional
    public void fullIndex(RepositoryRegistryEntity registry, String indexRunId) {
        log.info("[{}] Repository indexing started — repositoryId: '{}', project: '{}', repository: '{}'",
                indexRunId, registry.getRepositoryId(), registry.getProjectName(),
                registry.getRepositoryName());

        registry.setStatus(RepositoryStatus.INDEXING);
        registry.setFailureReason(null);
        registryRepository.save(registry);

        try {
            List<AzureRepositoryItem> items = azureDevOpsRepositoryService.listRepositoryItems(
                    registry.getProjectName(), registry.getRepositoryId(), registry.getDefaultBranch());

            List<AzureRepositoryItem> filesToIndex = items.stream()
                    .filter(i -> !i.isFolder())
                    .filter(i -> isSupported(i.getPath()))
                    .toList();

            log.info("[{}] Discovered {} supported file(s) — repositoryId: '{}'",
                    indexRunId, filesToIndex.size(), registry.getRepositoryId());

            // Wipe existing chunks for a clean full rebuild
            log.info("[{}] Clearing existing chunks for repository '{}'",
                    indexRunId, registry.getRepositoryName());

            int indexed = 0;
            int skipped = 0;
            for (AzureRepositoryItem item : filesToIndex) {
                try {
                    boolean wasIndexed = indexFile(registry, item.getPath(), indexRunId);
                    if (wasIndexed) indexed++; else skipped++;
                } catch (Exception e) {
                    log.warn("[{}] Skipping file '{}': {}", indexRunId, item.getPath(), e.getMessage());
                }
            }

            String latestCommit = azureDevOpsRepositoryService.getLatestCommitId(
                    registry.getProjectName(), registry.getRepositoryId(), registry.getDefaultBranch());

            long totalChunks = codeChunkRepository.countByRepository(registry.getRepositoryName());

            registry.setStatus(RepositoryStatus.INDEXED);
            registry.setLastIndexedCommit(latestCommit);
            registry.setLastIndexedAt(LocalDateTime.now());
            registry.setFileCount(indexed);
            registry.setChunkCount((int) totalChunks);
            registry.setIndexVersion(registry.getIndexVersion() + 1);
            registryRepository.save(registry);

            log.info("[{}] Repository indexing completed — repositoryId: '{}', files indexed: {}, " +
                     "files skipped: {}, chunks: {}, commit: {}",
                    indexRunId, registry.getRepositoryId(), indexed, skipped, totalChunks,
                    latestCommit.substring(0, 7));

        } catch (Exception e) {
            registry.setStatus(RepositoryStatus.FAILED);
            registry.setFailureReason(e.getMessage());
            registryRepository.save(registry);
            log.error("[{}] Repository indexing failed — repositoryId: '{}': {}",
                    indexRunId, registry.getRepositoryId(), e.getMessage(), e);
            throw new RepositoryIndexingException(
                    "Indexing failed for repo '%s'".formatted(registry.getRepositoryId()), e);
        }
    }

    @Override
    @Transactional
    public void incrementalSync(RepositoryRegistryEntity registry, String indexRunId) {
        log.info("[{}] Incremental sync started — repositoryId: '{}', lastCommit: {}",
                indexRunId, registry.getRepositoryId(),
                registry.getLastIndexedCommit() != null
                        ? registry.getLastIndexedCommit().substring(0, 7) : "none");

        registry.setStatus(RepositoryStatus.SYNC_IN_PROGRESS);
        registryRepository.save(registry);

        try {
            String latestCommit = azureDevOpsRepositoryService.getLatestCommitId(
                    registry.getProjectName(), registry.getRepositoryId(), registry.getDefaultBranch());

            if (latestCommit.equals(registry.getLastIndexedCommit())) {
                log.info("[{}] Repository '{}' is up-to-date at commit {}",
                        indexRunId, registry.getRepositoryId(), latestCommit.substring(0, 7));
                registry.setStatus(RepositoryStatus.INDEXED);
                registryRepository.save(registry);
                return;
            }

            // No prior index — fall back to full index
            if (registry.getLastIndexedCommit() == null) {
                log.info("[{}] No prior index for '{}' — running full index",
                        indexRunId, registry.getRepositoryId());
                fullIndex(registry, indexRunId);
                return;
            }

            List<AzureCommitChange> changes = azureDevOpsRepositoryService.getChangedFilesBetweenCommits(
                    registry.getProjectName(), registry.getRepositoryId(),
                    registry.getLastIndexedCommit(), latestCommit);

            log.info("[{}] Commit comparison completed — {} change(s) detected, commitId: {}",
                    indexRunId, changes.size(), latestCommit.substring(0, 7));

            int added = 0, updated = 0, deleted = 0;
            for (AzureCommitChange change : changes) {
                if (!isSupported(change.getPath())) continue;

                if (change.isDelete()) {
                    codeChunkRepository.deleteByRepositoryAndFilePath(
                            registry.getRepositoryName(), change.getPath());
                    log.info("[{}] Chunks removed — filePath: '{}', repositoryId: '{}'",
                            indexRunId, change.getPath(), registry.getRepositoryId());
                    deleted++;
                } else if (change.isAddOrEdit()) {
                    boolean wasNew = !codeChunkRepository
                            .findDistinctFilePathsByRepository(registry.getRepositoryName())
                            .contains(change.getPath());
                    indexFile(registry, change.getPath(), indexRunId);
                    if (wasNew) {
                        log.info("[{}] New file detected and indexed — filePath: '{}', repositoryId: '{}'",
                                indexRunId, change.getPath(), registry.getRepositoryId());
                        added++;
                    } else {
                        log.info("[{}] Modified file reindexed — filePath: '{}', repositoryId: '{}'",
                                indexRunId, change.getPath(), registry.getRepositoryId());
                        updated++;
                    }
                }
            }

            long totalChunks = codeChunkRepository.countByRepository(registry.getRepositoryName());

            registry.setStatus(RepositoryStatus.INDEXED);
            registry.setLastIndexedCommit(latestCommit);
            registry.setLastIndexedAt(LocalDateTime.now());
            registry.setChunkCount((int) totalChunks);
            registryRepository.save(registry);

            log.info("[{}] Incremental sync completed — repositoryId: '{}', added: {}, updated: {}, " +
                     "deleted: {}, chunks: {}, commitId: {}",
                    indexRunId, registry.getRepositoryId(), added, updated, deleted, totalChunks,
                    latestCommit.substring(0, 7));

        } catch (Exception e) {
            registry.setStatus(RepositoryStatus.FAILED);
            registry.setFailureReason(e.getMessage());
            registryRepository.save(registry);
            log.error("[{}] Incremental sync failed — repositoryId: '{}': {}",
                    indexRunId, registry.getRepositoryId(), e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    private boolean indexFile(RepositoryRegistryEntity registry, String filePath, String indexRunId) {
        String source = azureDevOpsService.getFileContent(
                registry.getProjectName(), registry.getRepositoryId(),
                filePath, registry.getDefaultBranch());

        if (source == null || source.isBlank()) {
            log.debug("[{}] Empty content for '{}' — skipping", indexRunId, filePath);
            return false;
        }

        if (source.length() > indexingProperties.getMaxFileSizeBytes()) {
            log.debug("[{}] File '{}' exceeds max size ({} bytes) — skipping",
                    indexRunId, filePath, source.length());
            return false;
        }

        CodeParser parser = codeParsers.stream()
                .filter(p -> p.supports(filePath))
                .findFirst()
                .orElse(null);

        if (parser == null) return false;

        List<ParsedSymbol> symbols = parser.parse(filePath, source);
        if (symbols.isEmpty()) {
            log.debug("[{}] No symbols in '{}' — skipping", indexRunId, filePath);
            return false;
        }

        List<CodeChunkEntity> candidates = codeChunker.chunk(registry.getRepositoryName(), symbols);

        // Keep existing unchanged chunks; only embed and persist new/changed ones
        List<CodeChunkEntity> toIndex = candidates.stream()
                .filter(c -> !codeChunkRepository.existsByRepositoryAndFilePathAndContentHash(
                        registry.getRepositoryName(), filePath, c.getContentHash()))
                .collect(Collectors.toList());

        if (toIndex.isEmpty()) {
            log.debug("[{}] All chunks for '{}' are unchanged — skipping embed", indexRunId, filePath);
            return false;
        }

        // Remove stale chunks before writing fresh ones
        codeChunkRepository.deleteByRepositoryAndFilePath(registry.getRepositoryName(), filePath);

        List<String> contents = toIndex.stream().map(CodeChunkEntity::getContent).toList();
        List<float[]> embeddings = embeddingService.embedBatch(contents);

        for (int i = 0; i < toIndex.size(); i++) {
            toIndex.get(i).setEmbedding(VectorUtil.toVectorString(embeddings.get(i)));
        }

        codeChunkRepository.saveAll(toIndex);
        log.debug("[{}] Embeddings generated and chunks inserted — filePath: '{}', chunks: {}, repositoryId: '{}'",
                indexRunId, filePath, toIndex.size(), registry.getRepositoryId());
        return true;
    }

    private boolean isSupported(String filePath) {
        if (filePath == null) return false;
        String lower = filePath.toLowerCase();
        return indexingProperties.getSupportedExtensions().stream().anyMatch(lower::endsWith);
    }
}
