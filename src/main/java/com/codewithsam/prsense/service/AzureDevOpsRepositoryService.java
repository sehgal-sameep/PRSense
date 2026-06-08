package com.codewithsam.prsense.service;

import com.codewithsam.prsense.model.AzureCommitChange;
import com.codewithsam.prsense.model.AzureRepositoryItem;

import java.util.List;

// Handles Azure DevOps REST API calls specific to repository discovery and indexing.
// Separate from AzureDevOpsService which focuses on PR review operations.
public interface AzureDevOpsRepositoryService {

    // Returns all items (files and folders) recursively from the repo root
    List<AzureRepositoryItem> listRepositoryItems(String project, String repoId, String branch);

    // Returns the SHA of the HEAD commit for a branch
    String getLatestCommitId(String project, String repoId, String branch);

    // Returns files changed between two commit SHAs
    List<AzureCommitChange> getChangedFilesBetweenCommits(String project, String repoId,
                                                           String fromCommit, String toCommit);

    // Returns the Azure DevOps repository GUID for a given project + repo name
    String resolveRepositoryId(String project, String repositoryName);

    // Returns the default branch name (e.g. "main") for a repository
    String getDefaultBranch(String project, String repoId);
}
