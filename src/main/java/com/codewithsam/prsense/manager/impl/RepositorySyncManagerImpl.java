package com.codewithsam.prsense.manager.impl;

import com.codewithsam.prsense.entity.RepositoryRegistryEntity;
import com.codewithsam.prsense.entity.RepositoryStatus;
import com.codewithsam.prsense.manager.RepositorySyncManager;
import com.codewithsam.prsense.repository.RepositoryRegistryRepository;
import com.codewithsam.prsense.service.RepositorySyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class RepositorySyncManagerImpl implements RepositorySyncManager {

    private final RepositorySyncService syncService;
    private final RepositoryRegistryRepository registryRepository;
    private final Executor reviewExecutor;

    public RepositorySyncManagerImpl(RepositorySyncService syncService,
                                      RepositoryRegistryRepository registryRepository,
                                      @Qualifier("reviewExecutor") Executor reviewExecutor) {
        this.syncService = syncService;
        this.registryRepository = registryRepository;
        this.reviewExecutor = reviewExecutor;
    }

    @Override
    public void syncByAzureRepoId(String project, String azureRepoId) {
        Optional<RepositoryRegistryEntity> found = registryRepository.findByRepositoryId(azureRepoId);

        if (found.isEmpty()) {
            log.debug("Push event for unregistered repository '{}' in project '{}' — ignored",
                    azureRepoId, project);
            return;
        }

        RepositoryRegistryEntity registry = found.get();
        String syncRunId = "SYNC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[{}] Webhook-triggered sync — repositoryId: '{}', project: '{}'",
                syncRunId, azureRepoId, project);

        reviewExecutor.execute(() -> {
            try {
                syncService.incrementalSync(registry, syncRunId);
            } catch (Exception e) {
                log.error("[{}] Webhook sync failed for '{}': {}",
                        syncRunId, azureRepoId, e.getMessage(), e);
            }
        });
    }

    @Override
    public void syncAll() {
        List<RepositoryRegistryEntity> eligible = registryRepository.findAllByStatusIn(
                List.of(RepositoryStatus.INDEXED, RepositoryStatus.STALE));

        if (eligible.isEmpty()) {
            log.debug("Scheduled sync — no eligible repositories");
            return;
        }

        log.info("Scheduled sync started — {} repository(ies) eligible", eligible.size());

        for (RepositoryRegistryEntity registry : eligible) {
            String syncRunId = "SCHED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            reviewExecutor.execute(() -> {
                try {
                    syncService.incrementalSync(registry, syncRunId);
                } catch (Exception e) {
                    log.error("[{}] Scheduled sync failed for '{}': {}",
                            syncRunId, registry.getRepositoryId(), e.getMessage(), e);
                }
            });
        }
    }
}
