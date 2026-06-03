package com.codewithsam.prsense.manager;

import com.codewithsam.prsense.dto.request.IndexRepositoryRequest;
import com.codewithsam.prsense.dto.response.IndexStatusResponse;

public interface RepositoryIndexManager {

    // Triggers async indexing and returns immediately
    void triggerIndexing(IndexRepositoryRequest request);

    IndexStatusResponse getStatus(String repositoryName);
}
