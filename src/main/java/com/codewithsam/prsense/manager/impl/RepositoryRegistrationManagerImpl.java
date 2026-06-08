package com.codewithsam.prsense.manager.impl;

import com.codewithsam.prsense.dto.request.RegisterRepositoryRequest;
import com.codewithsam.prsense.dto.request.ReindexRepositoryRequest;
import com.codewithsam.prsense.dto.response.RepositoryMetricsResponse;
import com.codewithsam.prsense.dto.response.RepositoryStatusResponse;
import com.codewithsam.prsense.entity.RepositoryRegistryEntity;
import com.codewithsam.prsense.manager.RepositoryRegistrationManager;
import com.codewithsam.prsense.repository.CodeChunkRepository;
import com.codewithsam.prsense.service.RepositoryRegistrationService;
import com.codewithsam.prsense.service.RepositorySyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class RepositoryRegistrationManagerImpl implements RepositoryRegistrationManager {

    private final RepositoryRegistrationService registrationService;
    private final RepositorySyncService syncService;
    private final CodeChunkRepository codeChunkRepository;
    private final Executor reviewExecutor;

    public RepositoryRegistrationManagerImpl(RepositoryRegistrationService registrationService,
                                              RepositorySyncService syncService,
                                              CodeChunkRepository codeChunkRepository,
                                              @Qualifier("reviewExecutor") Executor reviewExecutor) {
        this.registrationService = registrationService;
        this.syncService = syncService;
        this.codeChunkRepository = codeChunkRepository;
        this.reviewExecutor = reviewExecutor;
    }

    @Override
    public RepositoryStatusResponse register(RegisterRepositoryRequest request) {
        RepositoryStatusResponse response = registrationService.register(request);

        // Trigger initial full index asynchronously
        String indexRunId = "IDX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        reviewExecutor.execute(() -> {
            try {
                RepositoryRegistryEntity registry = registrationService.findByRepositoryId(
                        response.getRepositoryId());
                syncService.fullIndex(registry, indexRunId);
            } catch (Exception e) {
                log.error("[{}] Initial index failed for '{}': {}",
                        indexRunId, response.getRepositoryId(), e.getMessage(), e);
            }
        });

        log.info("Registration accepted — repositoryId: '{}', indexRunId: '{}'",
                response.getRepositoryId(), indexRunId);
        return response;
    }

    @Override
    public void reindex(ReindexRepositoryRequest request) {
        RepositoryRegistryEntity registry =
                registrationService.findByRepositoryId(request.getRepositoryId());

        String indexRunId = "REIDX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Reindex triggered — repositoryId: '{}', fullRebuild: {}, indexRunId: '{}'",
                request.getRepositoryId(), request.isFullRebuild(), indexRunId);

        reviewExecutor.execute(() -> {
            try {
                if (request.isFullRebuild()) {
                    syncService.fullIndex(registry, indexRunId);
                } else {
                    syncService.incrementalSync(registry, indexRunId);
                }
            } catch (Exception e) {
                log.error("[{}] Reindex failed for '{}': {}",
                        indexRunId, request.getRepositoryId(), e.getMessage(), e);
            }
        });
    }

    @Override
    public List<RepositoryStatusResponse> listRepositories() {
        return registrationService.listAll();
    }

    @Override
    public RepositoryStatusResponse getStatus(String repositoryId) {
        return registrationService.getStatus(repositoryId);
    }

    @Override
    public RepositoryMetricsResponse getMetrics(String repositoryId) {
        RepositoryStatusResponse status = registrationService.getStatus(repositoryId);
        long totalChunks = codeChunkRepository.countByRepository(status.getRepositoryName());

        return RepositoryMetricsResponse.builder()
                .repositoryId(repositoryId)
                .repositoryName(status.getRepositoryName())
                .totalChunks((int) totalChunks)
                .totalFiles(status.getFileCount())
                .indexVersion(status.getIndexVersion())
                .build();
    }
}
