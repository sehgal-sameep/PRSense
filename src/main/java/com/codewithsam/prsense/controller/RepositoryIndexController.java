package com.codewithsam.prsense.controller;

import com.codewithsam.prsense.dto.request.IndexRepositoryRequest;
import com.codewithsam.prsense.dto.response.IndexStatusResponse;
import com.codewithsam.prsense.manager.RepositoryIndexManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/index")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Indexing APIs", description = "APIs for triggering and monitoring repository code indexing")
public class RepositoryIndexController {

    private final RepositoryIndexManager repositoryIndexManager;

    @PostMapping("/repository")
    @Operation(
            summary = "Trigger repository indexing",
            description = "Scans the repository, parses source files, generates embeddings, and stores them in PGVector. Runs asynchronously."
    )
    public ResponseEntity<Void> indexRepository(@RequestBody @Valid IndexRepositoryRequest request) {
        log.info("POST /index/repository — repository: '{}'", request.getRepositoryName());
        repositoryIndexManager.triggerIndexing(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/status")
    @Operation(
            summary = "Get indexing status",
            description = "Returns chunk counts and last indexed timestamp for a repository"
    )
    public ResponseEntity<IndexStatusResponse> getStatus(
            @RequestParam String repositoryName) {
        log.debug("GET /index/status — repository: '{}'", repositoryName);
        return ResponseEntity.ok(repositoryIndexManager.getStatus(repositoryName));
    }
}
