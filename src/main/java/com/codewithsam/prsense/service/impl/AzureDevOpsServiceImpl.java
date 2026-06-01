package com.codewithsam.prsense.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.codewithsam.prsense.config.AzureDevOpsProperties;
import com.codewithsam.prsense.exception.AzureApiException;
import com.codewithsam.prsense.model.FileChange;
import com.codewithsam.prsense.model.PrDetails;
import com.codewithsam.prsense.service.AzureDevOpsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AzureDevOpsServiceImpl implements AzureDevOpsService {

    private final RestTemplate restTemplate;
    private final AzureDevOpsProperties azureDevOpsProperties;

    // ------------------------------------------------------------------ //
    //  Authentication
    // ------------------------------------------------------------------ //

    private HttpHeaders buildAuthHeaders() {
        String credentials = ":" + azureDevOpsProperties.getPat();
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encoded);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //

    @Override
    public PrDetails getPrDetails(String project, String repoId, int prId) {
        String url = azureDevOpsProperties.getBaseUrl()
                + "/{project}/_apis/git/repositories/{repoId}/pullrequests/{prId}?api-version=7.1";

        HttpEntity<?> entity = new HttpEntity<>(buildAuthHeaders());
        try {
            ResponseEntity<PrApiResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, PrApiResponse.class, project, repoId, prId);
            PrApiResponse body = response.getBody();
            if (body == null) {
                throw new AzureApiException("Empty response from PR details API for PR #" + prId);
            }
            return PrDetails.builder()
                    .title(body.getTitle())
                    .description(body.getDescription())
                    .author(body.getCreatedBy() != null ? body.getCreatedBy().getDisplayName() : "Unknown")
                    .sourceBranch(body.getSourceRefName())
                    .targetBranch(body.getTargetRefName())
                    .repoId(repoId)
                    .build();
        } catch (HttpStatusCodeException e) {
            throw new AzureApiException("Failed to get PR details for PR #" + prId
                    + " — HTTP " + e.getStatusCode(), e);
        }
    }

    @Override
    public List<FileChange> getChangedFiles(String project, String repoId, int prId) {
        int iterationId = getLatestIterationId(project, repoId, prId);
        String url = azureDevOpsProperties.getBaseUrl()
                + "/{project}/_apis/git/repositories/{repoId}/pullrequests/{prId}"
                + "/iterations/{iterationId}/changes?api-version=7.1";

        HttpEntity<?> entity = new HttpEntity<>(buildAuthHeaders());
        try {
            ResponseEntity<ChangesResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ChangesResponse.class,
                    project, repoId, prId, iterationId);
            ChangesResponse body = response.getBody();
            if (body == null || body.getChangeEntries() == null) {
                return List.of();
            }
            return body.getChangeEntries().stream()
                    .filter(e -> e.getItem() != null && !Boolean.TRUE.equals(e.getItem().getIsFolder()))
                    .map(e -> FileChange.builder()
                            .path(e.getItem().getPath())
                            .changeType(e.getChangeType())
                            .build())
                    .collect(Collectors.toList());
        } catch (HttpStatusCodeException e) {
            throw new AzureApiException("Failed to get changed files for PR #" + prId
                    + " — HTTP " + e.getStatusCode(), e);
        }
    }

    @Override
    public String getFileContent(String project, String repoId, String filePath, String branch) {
        String url = UriComponentsBuilder
                .fromUriString(azureDevOpsProperties.getBaseUrl())
                .pathSegment(project, "_apis", "git", "repositories", repoId, "items")
                .queryParam("path", filePath)
                .queryParam("versionDescriptor.version", branch)
                .queryParam("versionDescriptor.versionType", "branch")
                .queryParam("api-version", "7.1")
                .build()
                .toUriString();

        HttpHeaders headers = buildAuthHeaders();
        headers.setAccept(List.of(MediaType.TEXT_PLAIN, MediaType.ALL));
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody() != null ? response.getBody() : "";
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                // Expected for new files (no "before") or deleted files (no "after")
                log.debug("File not found on branch '{}': {}", branch, filePath);
                return "";
            }
            throw new AzureApiException("Failed to fetch content of " + filePath
                    + " on branch " + branch + " — HTTP " + e.getStatusCode(), e);
        } catch (HttpStatusCodeException e) {
            throw new AzureApiException("Failed to fetch content of " + filePath
                    + " — HTTP " + e.getStatusCode(), e);
        }
    }

    @Override
    public void postInlineComment(String project, String repoId, int prId,
                                  String filePath, String content, int lineNumber, String severity) {
        int targetLine = Math.max(1, lineNumber);

        String url = azureDevOpsProperties.getBaseUrl()
                + "/{project}/_apis/git/repositories/{repoId}/pullrequests/{prId}/threads?api-version=7.1";

        Map<String, Object> position = Map.of("line", targetLine, "offset", 1);
        Map<String, Object> threadContext = Map.of(
                "filePath", filePath,
                "rightFileStart", position,
                "rightFileEnd", position
        );

        String formattedContent = formatWithSeverity(content, severity);

        Map<String, Object> comment = Map.of("content", formattedContent, "commentType", 1);
        Map<String, Object> body = Map.of(
                "comments", List.of(comment),
                "status", 1,
                "threadContext", threadContext
        );

        HttpHeaders headers = buildAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class, project, repoId, prId);
            log.info("Posted [{}] comment on {}:{}", severity, filePath, targetLine);
        } catch (HttpStatusCodeException e) {
            log.warn("Line-level comment failed (line {}), falling back to file comment: {}", targetLine, e.getMessage());
            postGeneralFileComment(project, repoId, prId, filePath, formattedContent);
        }
    }

    @Override
    public void postPrSummaryComment(String project, String repoId, int prId, String summaryMarkdown) {
        String url = azureDevOpsProperties.getBaseUrl()
                + "/{project}/_apis/git/repositories/{repoId}/pullrequests/{prId}/threads?api-version=7.1";

        Map<String, Object> comment = Map.of("content", summaryMarkdown, "commentType", 1);
        Map<String, Object> body = Map.of("comments", List.of(comment), "status", 1);

        HttpHeaders headers = buildAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class, project, repoId, prId);
            log.info("Posted PR summary comment for PR #{}", prId);
        } catch (HttpStatusCodeException e) {
            log.error("Failed to post PR summary for PR #{} — HTTP {}", prId, e.getStatusCode());
        }
    }

    // ------------------------------------------------------------------ //
    //  Iteration helpers
    // ------------------------------------------------------------------ //

    @Override
    public int getLatestIterationId(String project, String repoId, int prId) {
        String url = azureDevOpsProperties.getBaseUrl()
                + "/{project}/_apis/git/repositories/{repoId}/pullrequests/{prId}/iterations?api-version=7.1";

        HttpEntity<?> entity = new HttpEntity<>(buildAuthHeaders());
        try {
            ResponseEntity<IterationsResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, IterationsResponse.class, project, repoId, prId);
            IterationsResponse body = response.getBody();
            if (body == null || body.getValue() == null || body.getValue().isEmpty()) {
                throw new AzureApiException("No iterations found for PR #" + prId);
            }
            List<IterationItem> iterations = body.getValue();
            return iterations.get(iterations.size() - 1).getId();
        } catch (HttpStatusCodeException e) {
            throw new AzureApiException("Failed to get iterations for PR #" + prId
                    + " — HTTP " + e.getStatusCode(), e);
        }
    }

    @Override
    public List<FileChange> getChangedFilesSinceIteration(String project, String repoId, int prId,
                                                          int latestIterationId, int sinceIterationId) {
        String url = UriComponentsBuilder
                .fromUriString(azureDevOpsProperties.getBaseUrl())
                .pathSegment(project, "_apis", "git", "repositories", repoId,
                        "pullrequests", String.valueOf(prId),
                        "iterations", String.valueOf(latestIterationId), "changes")
                .queryParam("$compareTo", sinceIterationId)
                .queryParam("api-version", "7.1")
                .build()
                .toUriString();

        HttpEntity<?> entity = new HttpEntity<>(buildAuthHeaders());
        try {
            ResponseEntity<ChangesResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ChangesResponse.class);
            ChangesResponse body = response.getBody();
            if (body == null || body.getChangeEntries() == null) return List.of();
            return body.getChangeEntries().stream()
                    .filter(e -> e.getItem() != null && !Boolean.TRUE.equals(e.getItem().getIsFolder()))
                    .map(e -> FileChange.builder()
                            .path(e.getItem().getPath())
                            .changeType(e.getChangeType())
                            .build())
                    .collect(Collectors.toList());
        } catch (HttpStatusCodeException e) {
            throw new AzureApiException("Failed to get files since iteration " + sinceIterationId
                    + " for PR #" + prId + " — HTTP " + e.getStatusCode(), e);
        }
    }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    // Prepends a severity emoji prefix so the comment is visually scannable in the Azure UI
    private String formatWithSeverity(String content, String severity) {
        if (severity == null || severity.isEmpty()) return content;
        String prefix = switch (severity.toUpperCase()) {
            case "CRITICAL", "HIGH" -> "🔴 [%s] ".formatted(severity);
            case "MEDIUM"           -> "🟠 [%s] ".formatted(severity);
            case "LOW"              -> "🟢 [%s] ".formatted(severity);
            default                 -> "[%s] ".formatted(severity);
        };
        return prefix + content;
    }

    // Falls back to a file-level comment when line-specific positioning fails
    private void postGeneralFileComment(String project, String repoId, int prId,
                                        String filePath, String content) {
        String url = azureDevOpsProperties.getBaseUrl()
                + "/{project}/_apis/git/repositories/{repoId}/pullrequests/{prId}/threads?api-version=7.1";

        Map<String, Object> position = Map.of("line", 1, "offset", 1);
        Map<String, Object> threadContext = Map.of(
                "filePath", filePath,
                "rightFileStart", position,
                "rightFileEnd", position
        );

        Map<String, Object> comment = Map.of("content", content, "commentType", 1);
        Map<String, Object> body = Map.of(
                "comments", List.of(comment),
                "status", 1,
                "threadContext", threadContext
        );

        HttpHeaders headers = buildAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class, project, repoId, prId);
            log.info("Posted file-level comment on: {}", filePath);
        } catch (HttpStatusCodeException e) {
            throw new AzureApiException("Failed to post comment on " + filePath
                    + " for PR #" + prId + " — HTTP " + e.getStatusCode(), e);
        }
    }

    // ------------------------------------------------------------------ //
    //  Internal response mapping classes (Azure API response shapes)
    // ------------------------------------------------------------------ //

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PrApiResponse {
        private String title;
        private String description;
        private CreatedBy createdBy;
        private String sourceRefName;
        private String targetRefName;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class CreatedBy {
            private String displayName;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class IterationsResponse {
        private List<IterationItem> value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class IterationItem {
        private int id;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChangesResponse {
        private List<ChangeEntry> changeEntries;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChangeEntry {
        private ChangeItem item;
        private String changeType;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class ChangeItem {
            private String path;
            @JsonProperty("isFolder")
            private Boolean isFolder;
        }
    }
}
