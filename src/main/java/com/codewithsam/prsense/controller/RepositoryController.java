package com.codewithsam.prsense.controller;

import com.codewithsam.prsense.dto.request.RegisterRepositoryRequest;
import com.codewithsam.prsense.dto.request.ReindexRepositoryRequest;
import com.codewithsam.prsense.dto.response.RepositoryMetricsResponse;
import com.codewithsam.prsense.dto.response.RepositoryStatusResponse;
import com.codewithsam.prsense.manager.RepositoryRegistrationManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/repositories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Repository APIs",
     description = "Register and manage Azure DevOps repositories for automatic AI-powered indexing")
public class RepositoryController {

    private final RepositoryRegistrationManager registrationManager;

    @PostMapping("/register")
    @Operation(
        summary = "Register a repository",
        description = "Registers an Azure DevOps repository for automatic indexing. " +
                      "Triggers initial full index immediately after registration."
    )
    public ResponseEntity<RepositoryStatusResponse> register(
            @RequestBody @Valid RegisterRepositoryRequest request) {
        log.info("POST /repositories/register — project: '{}', repository: '{}'",
                request.getProjectName(), request.getRepositoryName());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(registrationManager.register(request));
    }

    @PostMapping("/reindex")
    @Operation(
        summary = "Force reindex",
        description = "Triggers an immediate reindex of a registered repository. " +
                      "Set fullRebuild=true to wipe and rebuild from scratch."
    )
    public ResponseEntity<Void> reindex(@RequestBody @Valid ReindexRepositoryRequest request) {
        log.info("POST /repositories/reindex — repositoryId: '{}', fullRebuild: {}",
                request.getRepositoryId(), request.isFullRebuild());
        registrationManager.reindex(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping
    @Operation(summary = "List registered repositories")
    public ResponseEntity<List<RepositoryStatusResponse>> listRepositories() {
        return ResponseEntity.ok(registrationManager.listRepositories());
    }

    @GetMapping("/{repositoryId}/status")
    @Operation(summary = "Get repository indexing status")
    public ResponseEntity<RepositoryStatusResponse> getStatus(
            @PathVariable String repositoryId) {
        return ResponseEntity.ok(registrationManager.getStatus(repositoryId));
    }

    @GetMapping("/{repositoryId}/metrics")
    @Operation(summary = "Get repository indexing metrics")
    public ResponseEntity<RepositoryMetricsResponse> getMetrics(
            @PathVariable String repositoryId) {
        return ResponseEntity.ok(registrationManager.getMetrics(repositoryId));
    }
}
