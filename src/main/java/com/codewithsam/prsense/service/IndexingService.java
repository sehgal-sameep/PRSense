package com.codewithsam.prsense.service;

import com.codewithsam.prsense.dto.response.IndexStatusResponse;

// Drives the full indexing pipeline: scan → parse → chunk → embed → store
public interface IndexingService {

    void indexRepository(String repositoryName, String repositoryPath);

    IndexStatusResponse getIndexStatus(String repositoryName);
}
