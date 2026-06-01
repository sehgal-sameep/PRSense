package com.codewithsam.prsense.manager.impl;

import com.codewithsam.prsense.config.ReviewProperties;
import com.codewithsam.prsense.dto.request.WebhookPayloadRequest;
import com.codewithsam.prsense.dto.response.ReviewResponse;
import com.codewithsam.prsense.manager.WebhookManager;
import com.codewithsam.prsense.model.FileChange;
import com.codewithsam.prsense.model.FileDiff;
import com.codewithsam.prsense.model.LineComment;
import com.codewithsam.prsense.model.PrDetails;
import com.codewithsam.prsense.model.ReviewSummary;
import com.codewithsam.prsense.service.AzureDevOpsService;
import com.codewithsam.prsense.service.OpenAiService;
import com.codewithsam.prsense.util.CrossFileContextBuilder;
import com.codewithsam.prsense.util.LanguageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookManagerImpl implements WebhookManager {

    // Only these two Azure DevOps event types trigger a review
    private static final String EVENT_PR_CREATED = "git.pullrequest.created";
    private static final String EVENT_PR_UPDATED = "git.pullrequest.updated";

    private final AzureDevOpsService azureDevOpsService;
    private final OpenAiService openAiService;
    private final ReviewProperties reviewProperties;

    @Override
    public ReviewResponse handlePrWebhook(WebhookPayloadRequest payload) {
        String eventType = payload.getEventType();

        if (!EVENT_PR_CREATED.equals(eventType) && !EVENT_PR_UPDATED.equals(eventType)) {
            log.info("Ignoring unsupported event type: {}", eventType);
            return ReviewResponse.builder().status("ignored").build();
        }

        int prId = payload.getResource().getPullRequestId();
        String repoId = payload.getResource().getRepository().getId();
        String project = payload.getResource().getRepository().getProject().getName();

        log.info("Processing {} for PR #{} — project: {}, repo: {}", eventType, prId, project, repoId);

        PrDetails prDetails = azureDevOpsService.getPrDetails(project, repoId, prId);
        log.info("PR title: '{}'", prDetails.getTitle());

        // On updates, only review files changed in the latest push to avoid re-reviewing old code
        int latestIteration = azureDevOpsService.getLatestIterationId(project, repoId, prId);
        List<FileChange> allChanges;
        if (EVENT_PR_UPDATED.equals(eventType) && latestIteration > 1) {
            allChanges = azureDevOpsService.getChangedFilesSinceIteration(
                    project, repoId, prId, latestIteration, latestIteration - 1);
            log.info("PR update — iteration {}: {} file(s) changed since iteration {}",
                    latestIteration, allChanges.size(), latestIteration - 1);
        } else {
            allChanges = azureDevOpsService.getChangedFiles(project, repoId, prId);
            log.info("Total changed files: {}", allChanges.size());
        }

        // Filter out non-reviewable files (e.g. .yml, .json) and cap at configured max
        List<FileChange> filesToReview = allChanges.stream()
                .filter(f -> !shouldSkip(f.getPath()))
                .limit(reviewProperties.getMaxFiles())
                .collect(Collectors.toList());

        log.info("Files queued for review: {}", filesToReview.size());

        String sourceBranch = stripRefPrefix(prDetails.getSourceBranch());
        String targetBranch = stripRefPrefix(prDetails.getTargetBranch());

        // Pre-fetch all diffs so cross-file context can see every changed file before any review starts
        List<FileDiff> allDiffs = new ArrayList<>();
        for (FileChange fc : filesToReview) {
            FileDiff diff = fetchFileDiff(project, repoId, sourceBranch, targetBranch, fc);
            // Skip files where before == after (linter auto-reverted the change — nothing to review)
            if (isEffectivelyEmpty(diff)) {
                log.info("Skipping {} — before and after are identical", diff.getPath());
                continue;
            }
            allDiffs.add(diff);
        }

        String crossFileContext = CrossFileContextBuilder.build(allDiffs);
        if (!crossFileContext.isBlank()) {
            log.info("Cross-file context built ({} chars) for {} file(s)", crossFileContext.length(), allDiffs.size());
        }

        ReviewSummary summary = new ReviewSummary();
        int filesReviewed = 0;
        for (FileDiff fileDiff : allDiffs) {
            try {
                List<LineComment> comments = reviewSingleFile(prDetails, project, repoId, prId,
                        fileDiff, crossFileContext);
                summary.addFile(fileDiff.getPath(), comments);
                if (!comments.isEmpty()) filesReviewed++;
            } catch (Exception e) {
                log.error("Error reviewing file {}: {}", fileDiff.getPath(), e.getMessage());
            }
        }

        // Post the consolidated PR-level summary as one comment thread
        try {
            azureDevOpsService.postPrSummaryComment(project, repoId, prId, summary.toMarkdown(prId));
        } catch (Exception e) {
            log.error("Failed to post PR summary comment: {}", e.getMessage());
        }

        log.info("PR #{} review complete — {} file(s) had issues", prId, filesReviewed);
        return ReviewResponse.builder()
                .status("completed")
                .prId(prId)
                .filesReviewed(filesReviewed)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    // Fetches before/after content from Azure DevOps and builds a FileDiff — no AI call yet
    private FileDiff fetchFileDiff(String project, String repoId,
                                   String sourceBranch, String targetBranch,
                                   FileChange fileChange) {
        String path = fileChange.getPath();
        String before = truncate(
                azureDevOpsService.getFileContent(project, repoId, path, targetBranch),
                reviewProperties.getMaxLines());
        String after = truncate(
                azureDevOpsService.getFileContent(project, repoId, path, sourceBranch),
                reviewProperties.getMaxLines());
        return FileDiff.builder()
                .path(path)
                .changeType(fileChange.getChangeType())
                .before(before)
                .after(after)
                .lineCount(after.isEmpty() ? 0 : after.split("\n").length)
                .build();
    }

    // Sends the diff to the AI, then posts each returned comment as an inline thread on the PR
    private List<LineComment> reviewSingleFile(PrDetails prDetails, String project, String repoId,
                                               int prId, FileDiff fileDiff, String crossFileContext) {
        String path = fileDiff.getPath();
        log.info("Reviewing file: {}", path);

        List<LineComment> lineComments = openAiService.reviewFile(prDetails, fileDiff, crossFileContext);

        if (lineComments.isEmpty()) {
            log.info("No issues found in: {}", path);
            return lineComments;
        }

        String codeFence = LanguageUtil.toCodeFence(path);
        int commentsPosted = 0;
        for (LineComment lineComment : lineComments) {
            try {
                azureDevOpsService.postInlineComment(
                        project, repoId, prId, path,
                        buildCommentBody(lineComment, codeFence),
                        lineComment.getLine(),
                        lineComment.getSeverity()
                );
                commentsPosted++;
            } catch (Exception e) {
                log.error("Failed to post comment on line {} for {}: {}",
                        lineComment.getLine(), path, e.getMessage());
            }
        }

        log.info("Posted {}/{} comment(s) for: {}", commentsPosted, lineComments.size(), path);
        return lineComments;
    }

    // Returns true if before == after after normalising line endings — nothing meaningful changed
    private boolean isEffectivelyEmpty(FileDiff diff) {
        String before = diff.getBefore() == null ? "" : diff.getBefore().stripTrailing().replace("\r\n", "\n");
        String after  = diff.getAfter()  == null ? "" : diff.getAfter().stripTrailing().replace("\r\n", "\n");
        return before.equals(after);
    }

    // Skips files with extensions listed in review.skip-extensions (e.g. .yml, .json, .md)
    private boolean shouldSkip(String path) {
        if (path == null) return true;
        String lower = path.toLowerCase();
        return reviewProperties.getSkipExtensions().stream()
                .anyMatch(ext -> lower.endsWith(ext.trim()));
    }

    // Strips "refs/heads/" prefix from Azure DevOps branch refs to get the plain branch name
    private String stripRefPrefix(String ref) {
        if (ref != null && ref.startsWith("refs/heads/")) {
            return ref.substring("refs/heads/".length());
        }
        return ref;
    }

    // Caps file content at maxLines to stay within the AI token budget
    private String truncate(String content, int maxLines) {
        if (content == null || content.isEmpty()) return "";
        String[] lines = content.split("\n");
        if (lines.length <= maxLines) return content;
        return String.join("\n", Arrays.copyOf(lines, maxLines))
                + "\n... (truncated to " + maxLines + " lines)";
    }

    // Appends the AI's code suggestion as a fenced block below the comment text
    private String buildCommentBody(LineComment lineComment, String codeFence) {
        String suggestion = lineComment.getSuggestion();
        if (suggestion == null || suggestion.isBlank() || suggestion.equalsIgnoreCase("null")) {
            return lineComment.getComment();
        }
        return lineComment.getComment()
                + "\n\n💡 **Suggested fix:**\n```" + codeFence + "\n"
                + suggestion.trim()
                + "\n```";
    }
}
