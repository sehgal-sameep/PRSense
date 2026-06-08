package com.codewithsam.prsense.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.codewithsam.prsense.config.AzureDevOpsProperties;
import com.codewithsam.prsense.exception.AzureApiException;
import com.codewithsam.prsense.model.AzureCommitChange;
import com.codewithsam.prsense.model.AzureRepositoryItem;
import com.codewithsam.prsense.service.AzureDevOpsRepositoryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AzureDevOpsRepositoryServiceImpl implements AzureDevOpsRepositoryService {

    private final RestTemplate restTemplate;
    private final AzureDevOpsProperties azureDevOpsProperties;

    @Override
    public List<AzureRepositoryItem> listRepositoryItems(String project, String repoId, String branch) {
        String url = UriComponentsBuilder
                .fromUriString(azureDevOpsProperties.getBaseUrl())
                .pathSegment(project, "_apis", "git", "repositories", repoId, "items")
                .queryParam("scopePath", "/")
                .queryParam("recursionLevel", "Full")
                .queryParam("versionDescriptor.version", branch)
                .queryParam("versionDescriptor.versionType", "branch")
                .queryParam("api-version", "7.1")
                .build().toUriString();

        try {
            ResponseEntity<ItemsResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, authEntity(), ItemsResponse.class);

            List<ItemsResponse.ItemEntry> entries = Objects.requireNonNull(response.getBody()).getValue();
            if (entries == null) return List.of();

            List<AzureRepositoryItem> items = entries.stream()
                    .map(e -> AzureRepositoryItem.builder()
                            .path(e.getPath())
                            .folder(Boolean.TRUE.equals(e.getIsFolder()))
                            .commitId(e.getCommitId())
                            .gitObjectType(e.getGitObjectType())
                            .build())
                    .collect(Collectors.toList());

            log.info("Listed {} item(s) in repository '{}' on branch '{}'",
                    items.size(), repoId, branch);
            return items;

        } catch (HttpStatusCodeException e) {
            throw new AzureApiException(
                    "Failed to list items for repo '%s' — HTTP %s".formatted(repoId, e.getStatusCode()), e);
        }
    }

    @Override
    public String getLatestCommitId(String project, String repoId, String branch) {
        String url = UriComponentsBuilder
                .fromUriString(azureDevOpsProperties.getBaseUrl())
                .pathSegment(project, "_apis", "git", "repositories", repoId, "commits")
                .queryParam("searchCriteria.itemVersion.version", branch)
                .queryParam("searchCriteria.$top", "1")
                .queryParam("api-version", "7.1")
                .build().toUriString();

        try {
            ResponseEntity<CommitsResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, authEntity(), CommitsResponse.class);

            List<CommitsResponse.CommitEntry> commits =
                    Objects.requireNonNull(response.getBody()).getValue();
            if (commits == null || commits.isEmpty()) {
                throw new AzureApiException(
                        "No commits found for repo '%s' branch '%s'".formatted(repoId, branch));
            }

            String commitId = commits.get(0).getCommitId();
            log.debug("Latest commit for '{}' on '{}': {}", repoId, branch, commitId);
            return commitId;

        } catch (HttpStatusCodeException e) {
            throw new AzureApiException(
                    "Failed to get latest commit for repo '%s' — HTTP %s"
                            .formatted(repoId, e.getStatusCode()), e);
        }
    }

    @Override
    public List<AzureCommitChange> getChangedFilesBetweenCommits(String project, String repoId,
                                                                   String fromCommit, String toCommit) {
        String url = UriComponentsBuilder
                .fromUriString(azureDevOpsProperties.getBaseUrl())
                .pathSegment(project, "_apis", "git", "repositories", repoId, "diffs", "commits")
                .queryParam("baseVersion", fromCommit)
                .queryParam("baseVersionType", "commit")
                .queryParam("targetVersion", toCommit)
                .queryParam("targetVersionType", "commit")
                .queryParam("api-version", "7.1")
                .build().toUriString();

        try {
            ResponseEntity<DiffResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, authEntity(), DiffResponse.class);

            List<DiffResponse.ChangeEntry> changes =
                    Objects.requireNonNull(response.getBody()).getChanges();
            if (changes == null) return List.of();

            List<AzureCommitChange> result = changes.stream()
                    .filter(c -> c.getItem() != null && !Boolean.TRUE.equals(c.getItem().getIsFolder()))
                    .map(c -> AzureCommitChange.builder()
                            .path(c.getItem().getPath())
                            .changeType(c.getChangeType())
                            .build())
                    .collect(Collectors.toList());

            log.info("Diff {}->{} in '{}': {} file change(s)",
                    fromCommit.substring(0, 7), toCommit.substring(0, 7), repoId, result.size());
            return result;

        } catch (HttpStatusCodeException e) {
            throw new AzureApiException(
                    "Failed to diff commits %s->%s in repo '%s' — HTTP %s"
                            .formatted(fromCommit, toCommit, repoId, e.getStatusCode()), e);
        }
    }

    @Override
    public String resolveRepositoryId(String project, String repositoryName) {
        String url = azureDevOpsProperties.getBaseUrl()
                + "/{project}/_apis/git/repositories/{name}?api-version=7.1";

        try {
            ResponseEntity<RepoDetailResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, authEntity(), RepoDetailResponse.class,
                    project, repositoryName);

            String id = Objects.requireNonNull(response.getBody()).getId();
            log.debug("Resolved repository '{}' in project '{}' to ID: {}", repositoryName, project, id);
            return id;

        } catch (HttpStatusCodeException e) {
            throw new AzureApiException(
                    "Failed to resolve repo '%s' in project '%s' — HTTP %s"
                            .formatted(repositoryName, project, e.getStatusCode()), e);
        }
    }

    @Override
    public String getDefaultBranch(String project, String repoId) {
        String url = azureDevOpsProperties.getBaseUrl()
                + "/{project}/_apis/git/repositories/{repoId}?api-version=7.1";

        try {
            ResponseEntity<RepoDetailResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, authEntity(), RepoDetailResponse.class, project, repoId);

            String ref = Objects.requireNonNull(response.getBody()).getDefaultBranch();
            // Azure returns "refs/heads/main" — strip the prefix
            return ref != null && ref.startsWith("refs/heads/")
                    ? ref.substring("refs/heads/".length()) : ref;

        } catch (HttpStatusCodeException e) {
            log.warn("Could not determine default branch for repo '{}', defaulting to 'main': {}",
                    repoId, e.getMessage());
            return "main";
        }
    }

    // ------------------------------------------------------------------ //
    //  Auth helper
    // ------------------------------------------------------------------ //

    private HttpEntity<?> authEntity() {
        String encoded = Base64.getEncoder().encodeToString(
                (":" + azureDevOpsProperties.getPat()).getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encoded);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }

    // ------------------------------------------------------------------ //
    //  Internal response mapping
    // ------------------------------------------------------------------ //

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    static class ItemsResponse {
        private List<ItemEntry> value;

        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        static class ItemEntry {
            private String path;
            @JsonProperty("isFolder") private Boolean isFolder;
            private String commitId;
            private String gitObjectType;
        }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    static class CommitsResponse {
        private List<CommitEntry> value;

        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        static class CommitEntry {
            private String commitId;
        }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    static class DiffResponse {
        private List<ChangeEntry> changes;

        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        static class ChangeEntry {
            private ChangeItem item;
            private String changeType;

            @Data @JsonIgnoreProperties(ignoreUnknown = true)
            static class ChangeItem {
                private String path;
                @JsonProperty("isFolder") private Boolean isFolder;
            }
        }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    static class RepoDetailResponse {
        private String id;
        private String name;
        private String defaultBranch;
    }
}
