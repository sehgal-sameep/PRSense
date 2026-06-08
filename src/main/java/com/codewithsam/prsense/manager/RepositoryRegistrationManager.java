package com.codewithsam.prsense.manager;

import com.codewithsam.prsense.dto.request.RegisterRepositoryRequest;
import com.codewithsam.prsense.dto.request.ReindexRepositoryRequest;
import com.codewithsam.prsense.dto.response.RepositoryMetricsResponse;
import com.codewithsam.prsense.dto.response.RepositoryStatusResponse;

import java.util.List;

public interface RepositoryRegistrationManager {

    // Registers a repository and triggers initial async indexing
    RepositoryStatusResponse register(RegisterRepositoryRequest request);

    // Forces a full or incremental reindex of a registered repository
    void reindex(ReindexRepositoryRequest request);

    List<RepositoryStatusResponse> listRepositories();

    RepositoryStatusResponse getStatus(String repositoryId);

    RepositoryMetricsResponse getMetrics(String repositoryId);
}
