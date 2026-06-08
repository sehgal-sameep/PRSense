package com.codewithsam.prsense.scheduler;

import com.codewithsam.prsense.manager.RepositorySyncManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Periodically checks all registered repositories for new commits and syncs them.
// Disabled when indexing.sync-enabled=false (e.g. in test environments).
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "indexing.sync-enabled", havingValue = "true", matchIfMissing = true)
public class RepositorySyncScheduler {

    private final RepositorySyncManager repositorySyncManager;

    @Scheduled(fixedDelayString = "${indexing.sync-interval-ms:900000}",
               initialDelayString = "${indexing.sync-initial-delay-ms:60000}")
    public void scheduledSync() {
        log.info("Scheduled repository sync started");
        try {
            repositorySyncManager.syncAll();
        } catch (Exception e) {
            log.error("Scheduled sync encountered an error: {}", e.getMessage(), e);
        }
    }
}
