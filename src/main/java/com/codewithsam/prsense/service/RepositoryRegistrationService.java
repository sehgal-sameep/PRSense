package com.codewithsam.prsense.service;

import com.codewithsam.prsense.dto.request.RegisterRepositoryRequest;
import com.codewithsam.prsense.dto.response.RepositoryStatusResponse;
import com.codewithsam.prsense.entity.RepositoryRegistryEntity;

import java.util.List;

public interface RepositoryRegistrationService {

    RepositoryStatusResponse register(RegisterRepositoryRequest request);

    List<RepositoryStatusResponse> listAll();

    RepositoryStatusResponse getStatus(String repositoryId);

    RepositoryRegistryEntity findByRepositoryId(String repositoryId);

    RepositoryRegistryEntity findByProjectAndName(String projectName, String repositoryName);
}
