package com.codewithsam.prsense.controller;

import com.codewithsam.prsense.dto.response.CodeSearchResponse;
import com.codewithsam.prsense.manager.CodeSearchManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Search APIs", description = "APIs for semantic code retrieval using vector similarity")
public class CodeSearchController {

    private final CodeSearchManager codeSearchManager;

    @GetMapping("/code")
    @Operation(
            summary = "Semantic code search",
            description = "Returns the top N most semantically similar code chunks from the indexed repository"
    )
    public ResponseEntity<CodeSearchResponse> searchCode(
            @RequestParam @NotBlank(message = "query is required") String query,
            @RequestParam String repositoryName,
            @RequestParam(defaultValue = "0") int limit) {

        log.info("GET /search/code — repository: '{}', query: '{}'", repositoryName, query);
        return ResponseEntity.ok(codeSearchManager.search(query, repositoryName, limit));
    }
}
