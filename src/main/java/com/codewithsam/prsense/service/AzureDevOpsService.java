package com.codewithsam.prsense.service;

import com.codewithsam.prsense.model.FileChange;
import com.codewithsam.prsense.model.PrDetails;

import java.util.List;

// Handles all communication with Azure DevOps REST API
public interface AzureDevOpsService {

    // Fetches PR title, description, author, and branch names
    PrDetails getPrDetails(String project, String repoId, int prId);

    // Returns all files changed across the latest iteration of the PR
    List<FileChange> getChangedFiles(String project, String repoId, int prId);

    // Fetches raw file content from a specific branch; returns empty string if file doesn't exist
    String getFileContent(String project, String repoId, String filePath, String branch);

    // Posts an inline comment thread on a specific file and line in the PR
    void postInlineComment(String project, String repoId, int prId,
                          String filePath, String content, int lineNumber, String severity);

    // Posts the overall AI review summary as a PR-level comment thread
    void postPrSummaryComment(String project, String repoId, int prId, String summaryMarkdown);

    // Returns the latest iteration ID; used to determine if the PR was re-pushed
    int getLatestIterationId(String project, String repoId, int prId);

    // Returns only files changed between two iterations — avoids re-reviewing unchanged files on updates
    List<FileChange> getChangedFilesSinceIteration(String project, String repoId, int prId,
                                                   int latestIterationId, int sinceIterationId);
}
