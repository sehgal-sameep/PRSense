package com.codewithsam.prsense.service;

import com.codewithsam.prsense.entity.RepositoryRegistryEntity;

// Drives the full and incremental indexing pipeline against Azure DevOps.
// Called by managers for initial indexing and webhook/scheduled sync.
public interface RepositorySyncService {

    // Full index: fetches all supported files and indexes from scratch
    void fullIndex(RepositoryRegistryEntity registry, String indexRunId);

    // Incremental sync: fetches only files changed since lastIndexedCommit
    void incrementalSync(RepositoryRegistryEntity registry, String indexRunId);
}
