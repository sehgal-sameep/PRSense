package com.codewithsam.prsense.manager.impl;

import com.codewithsam.prsense.dto.request.IndexRepositoryRequest;
import com.codewithsam.prsense.dto.response.IndexStatusResponse;
import com.codewithsam.prsense.manager.RepositoryIndexManager;
import com.codewithsam.prsense.service.IndexingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
@Slf4j
public class RepositoryIndexManagerImpl implements RepositoryIndexManager {

    private final IndexingService indexingService;
    private final Executor reviewExecutor;

    public RepositoryIndexManagerImpl(IndexingService indexingService,
                                      @Qualifier("reviewExecutor") Executor reviewExecutor) {
        this.indexingService = indexingService;
        this.reviewExecutor = reviewExecutor;
    }

    @Override
    public void triggerIndexing(IndexRepositoryRequest request) {
        log.info("Indexing triggered — repository: '{}', path: '{}'",
                request.getRepositoryName(), request.getRepositoryPath());

        reviewExecutor.execute(() -> {
            try {
                indexingService.indexRepository(request.getRepositoryName(), request.getRepositoryPath());
            } catch (Exception e) {
                log.error("Async indexing failed for repository '{}': {}",
                        request.getRepositoryName(), e.getMessage(), e);
            }
        });
    }

    @Override
    public IndexStatusResponse getStatus(String repositoryName) {
        log.debug("Fetching index status for repository '{}'", repositoryName);
        return indexingService.getIndexStatus(repositoryName);
    }
}
