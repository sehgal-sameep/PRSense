package com.codewithsam.prsense.service.impl;

import com.codewithsam.prsense.dto.request.RegisterRepositoryRequest;
import com.codewithsam.prsense.dto.response.RepositoryStatusResponse;
import com.codewithsam.prsense.entity.RepositoryRegistryEntity;
import com.codewithsam.prsense.entity.RepositoryStatus;
import com.codewithsam.prsense.exception.ResourceNotFoundException;
import com.codewithsam.prsense.repository.RepositoryRegistryRepository;
import com.codewithsam.prsense.service.AzureDevOpsRepositoryService;
import com.codewithsam.prsense.service.RepositoryRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryRegistrationServiceImpl implements RepositoryRegistrationService {

    private final RepositoryRegistryRepository registryRepository;
    private final AzureDevOpsRepositoryService azureDevOpsRepositoryService;

    @Override
    public RepositoryStatusResponse register(RegisterRepositoryRequest request) {
        log.info("Repository registration started — project: '{}', repository: '{}'",
                request.getProjectName(), request.getRepositoryName());

        if (registryRepository.existsByProjectNameAndRepositoryName(
                request.getProjectName(), request.getRepositoryName())) {
            log.warn("Repository '{}' in project '{}' is already registered",
                    request.getRepositoryName(), request.getProjectName());
            RepositoryRegistryEntity existing = registryRepository
                    .findByProjectNameAndRepositoryName(
                            request.getProjectName(), request.getRepositoryName())
                    .orElseThrow();
            return toResponse(existing);
        }

        // Resolve Azure DevOps repo GUID and default branch
        String repoId = azureDevOpsRepositoryService.resolveRepositoryId(
                request.getProjectName(), request.getRepositoryName());
        String branch = request.getBranch() != null
                ? request.getBranch()
                : azureDevOpsRepositoryService.getDefaultBranch(request.getProjectName(), repoId);

        RepositoryRegistryEntity entity = RepositoryRegistryEntity.builder()
                .repositoryId(repoId)
                .projectName(request.getProjectName())
                .repositoryName(request.getRepositoryName())
                .defaultBranch(branch)
                .status(RepositoryStatus.REGISTERED)
                .chunkCount(0)
                .fileCount(0)
                .indexVersion(0)
                .build();

        registryRepository.save(entity);
        log.info("Repository registration completed — repositoryId: '{}', repositoryName: '{}', branch: '{}'",
                repoId, request.getRepositoryName(), branch);

        return toResponse(entity);
    }

    @Override
    public List<RepositoryStatusResponse> listAll() {
        return registryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public RepositoryStatusResponse getStatus(String repositoryId) {
        return toResponse(findByRepositoryId(repositoryId));
    }

    @Override
    public RepositoryRegistryEntity findByRepositoryId(String repositoryId) {
        return registryRepository.findByRepositoryId(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Repository not registered: " + repositoryId));
    }

    @Override
    public RepositoryRegistryEntity findByProjectAndName(String projectName, String repositoryName) {
        return registryRepository.findByProjectNameAndRepositoryName(projectName, repositoryName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Repository not registered: " + projectName + "/" + repositoryName));
    }

    private RepositoryStatusResponse toResponse(RepositoryRegistryEntity e) {
        return RepositoryStatusResponse.builder()
                .repositoryId(e.getRepositoryId())
                .projectName(e.getProjectName())
                .repositoryName(e.getRepositoryName())
                .defaultBranch(e.getDefaultBranch())
                .status(e.getStatus())
                .lastIndexedCommit(e.getLastIndexedCommit())
                .lastIndexedAt(e.getLastIndexedAt())
                .chunkCount(e.getChunkCount())
                .fileCount(e.getFileCount())
                .indexVersion(e.getIndexVersion())
                .failureReason(e.getFailureReason())
                .build();
    }
}
