package com.codewithsam.prsense.manager.impl;

import com.codewithsam.prsense.config.ReviewProperties;
import com.codewithsam.prsense.dto.request.ReviewRequest;
import com.codewithsam.prsense.manager.ReviewManager;
import com.codewithsam.prsense.model.*;
import com.codewithsam.prsense.service.AzureDevOpsService;
import com.codewithsam.prsense.service.AgentReviewService;
import com.codewithsam.prsense.service.ReviewHistoryService;
import com.codewithsam.prsense.util.CrossFileContextBuilder;
import com.codewithsam.prsense.util.LanguageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ReviewManagerImpl implements ReviewManager {

    private final AzureDevOpsService azureDevOpsService;
    private final AgentReviewService agentReviewService;
    private final ReviewHistoryService reviewHistoryService;
    private final ReviewProperties reviewProperties;
    private final Executor reviewExecutor;

    public ReviewManagerImpl(AzureDevOpsService azureDevOpsService,
                             AgentReviewService agentReviewService,
                             ReviewHistoryService reviewHistoryService,
                             ReviewProperties reviewProperties,
                             @Qualifier("reviewExecutor") Executor reviewExecutor) {
        this.azureDevOpsService = azureDevOpsService;
        this.agentReviewService = agentReviewService;
        this.reviewHistoryService = reviewHistoryService;
        this.reviewProperties = reviewProperties;
        this.reviewExecutor = reviewExecutor;
    }

    @Override
    public String processReview(ReviewRequest request) {
        String reviewId = "REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ReviewRecord record = ReviewRecord.builder()
                .reviewId(reviewId)
                .triggerType(request.getTriggerType())
                .repositoryId(request.getRepositoryId())
                .pullRequestId(request.getPullRequestId())
                .projectName(request.getProjectName())
                .startTime(LocalDateTime.now())
                .status(ReviewStatus.QUEUED)
                .build();

        reviewHistoryService.save(record);
        log.info("Review {} queued — PR #{} via {}", reviewId, request.getPullRequestId(), request.getTriggerType());

        reviewExecutor.execute(() -> executeReview(record));
        return reviewId;
    }

    @Override
    public ReviewRecord getReview(String reviewId) {
        return reviewHistoryService.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
    }

    @Override
    public List<ReviewRecord> getReviewHistory(String repositoryId, Integer pullRequestId,
                                               LocalDateTime from, LocalDateTime to, ReviewStatus status) {
        return reviewHistoryService.findAll(repositoryId, pullRequestId, from, to, status);
    }

    // ------------------------------------------------------------------ //
    //  Async execution
    // ------------------------------------------------------------------ //

    private void executeReview(ReviewRecord record) {
        record.setStatus(ReviewStatus.IN_PROGRESS);
        reviewHistoryService.update(record);

        try {
            ReviewResult result = runReview(record);
            record.setStatus(ReviewStatus.COMPLETED);
            record.setEndTime(LocalDateTime.now());
            record.setTotalFilesReviewed(result.filesReviewed());
            record.setTotalCommentsGenerated(result.commentsGenerated());
            log.info("Review {} completed — {} file(s), {} comment(s)",
                    record.getReviewId(), result.filesReviewed(), result.commentsGenerated());
        } catch (Exception e) {
            record.setStatus(ReviewStatus.FAILED);
            record.setEndTime(LocalDateTime.now());
            record.setFailureReason(e.getMessage());
            log.error("Review {} failed: {}", record.getReviewId(), e.getMessage(), e);
        } finally {
            reviewHistoryService.update(record);
        }
    }

    private ReviewResult runReview(ReviewRecord record) {
        String project = record.getProjectName();
        String repoId = record.getRepositoryId();
        int prId = record.getPullRequestId();

        PrDetails prDetails = azureDevOpsService.getPrDetails(project, repoId, prId);
        log.info("[{}] Reviewing PR #{} — '{}'", record.getReviewId(), prId, prDetails.getTitle());

        int latestIteration = azureDevOpsService.getLatestIterationId(project, repoId, prId);
        List<FileChange> allChanges;

        // On subsequent iterations, only review files changed since the previous push
        if (latestIteration > 1) {
            allChanges = azureDevOpsService.getChangedFilesSinceIteration(
                    project, repoId, prId, latestIteration, latestIteration - 1);
            log.info("[{}] Iteration {}: {} file(s) changed", record.getReviewId(), latestIteration, allChanges.size());
        } else {
            allChanges = azureDevOpsService.getChangedFiles(project, repoId, prId);
            log.info("[{}] {} file(s) changed in PR", record.getReviewId(), allChanges.size());
        }

        List<FileChange> filesToReview = allChanges.stream()
                .filter(f -> !shouldSkip(f.getPath()))
                .limit(reviewProperties.getMaxFiles())
                .collect(Collectors.toList());

        log.info("[{}] {} file(s) eligible for review", record.getReviewId(), filesToReview.size());

        String sourceBranch = stripRefPrefix(prDetails.getSourceBranch());
        String targetBranch = stripRefPrefix(prDetails.getTargetBranch());

        // Fetch all diffs up front so cross-file context sees the full picture before any review starts
        List<FileDiff> allDiffs = new ArrayList<>();
        for (FileChange fc : filesToReview) {
            FileDiff diff = fetchFileDiff(project, repoId, sourceBranch, targetBranch, fc);
            if (isEffectivelyEmpty(diff)) {
                log.info("[{}] Skipping {} — before and after are identical", record.getReviewId(), diff.getPath());
                continue;
            }
            allDiffs.add(diff);
        }

        String crossFileContext = CrossFileContextBuilder.build(allDiffs);
        if (!crossFileContext.isBlank()) {
            log.info("[{}] Cross-file context: {} chars across {} file(s)",
                    record.getReviewId(), crossFileContext.length(), allDiffs.size());
        }

        // Collect all changed file paths so the agent can pass them to retrieve_context
        List<String> allChangedFilePaths = allDiffs.stream().map(FileDiff::getPath).toList();
        log.info("[{}] Passing {} file path(s) to agent for tool-assisted context retrieval",
                record.getReviewId(), allChangedFilePaths.size());

        ReviewSummary summary = new ReviewSummary();
        int filesReviewed = 0;
        int totalComments = 0;

        for (FileDiff fileDiff : allDiffs) {
            try {
                List<LineComment> comments = reviewSingleFile(
                        prDetails, project, repoId, prId, fileDiff, crossFileContext, allChangedFilePaths);
                summary.addFile(fileDiff.getPath(), comments);
                if (!comments.isEmpty()) {
                    filesReviewed++;
                    totalComments += comments.size();
                }
            } catch (Exception e) {
                log.error("[{}] Error reviewing {}: {}", record.getReviewId(), fileDiff.getPath(), e.getMessage());
            }
        }

        try {
            azureDevOpsService.postPrSummaryComment(project, repoId, prId, summary.toMarkdown(prId));
        } catch (Exception e) {
            log.error("[{}] Failed to post PR summary: {}", record.getReviewId(), e.getMessage());
        }

        return new ReviewResult(filesReviewed, totalComments);
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

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

    private List<LineComment> reviewSingleFile(PrDetails prDetails, String project, String repoId,
                                               int prId, FileDiff fileDiff, String crossFileContext,
                                               List<String> allChangedFilePaths) {
        String path = fileDiff.getPath();
        log.info("Reviewing: {}", path);

        // Agent call — LLM decides which MCP tools to invoke before generating the review.
        // Confirm tool usage by watching for "Tool execution started" logs immediately after this line.
        List<LineComment> lineComments = agentReviewService.reviewFile(
                prDetails, fileDiff, crossFileContext, project, allChangedFilePaths);

        if (lineComments.isEmpty()) {
            log.info("No issues in: {}", path);
            return lineComments;
        }

        String codeFence = LanguageUtil.toCodeFence(path);
        int posted = 0;
        for (LineComment comment : lineComments) {
            try {
                azureDevOpsService.postInlineComment(
                        project, repoId, prId, path,
                        buildCommentBody(comment, codeFence),
                        comment.getLine(),
                        comment.getSeverity());
                posted++;
            } catch (Exception e) {
                log.error("Failed to post comment on line {} for {}: {}", comment.getLine(), path, e.getMessage());
            }
        }

        log.info("Posted {}/{} comment(s) for: {}", posted, lineComments.size(), path);
        return lineComments;
    }

    private boolean isEffectivelyEmpty(FileDiff diff) {
        String before = diff.getBefore() == null ? "" : diff.getBefore().stripTrailing().replace("\r\n", "\n");
        String after  = diff.getAfter()  == null ? "" : diff.getAfter().stripTrailing().replace("\r\n", "\n");
        return before.equals(after);
    }

    private boolean shouldSkip(String path) {
        if (path == null) return true;
        String lower = path.toLowerCase();
        return reviewProperties.getSkipExtensions().stream().anyMatch(ext -> lower.endsWith(ext.trim()));
    }

    private String stripRefPrefix(String ref) {
        if (ref != null && ref.startsWith("refs/heads/")) {
            return ref.substring("refs/heads/".length());
        }
        return ref;
    }

    private String truncate(String content, int maxLines) {
        if (content == null || content.isEmpty()) return "";
        String[] lines = content.split("\n");
        if (lines.length <= maxLines) return content;
        return String.join("\n", Arrays.copyOf(lines, maxLines))
                + "\n... (truncated to " + maxLines + " lines)";
    }

    private String buildCommentBody(LineComment comment, String codeFence) {
        String suggestion = comment.getSuggestion();
        if (suggestion == null || suggestion.isBlank() || suggestion.equalsIgnoreCase("null")) {
            return comment.getComment();
        }
        return comment.getComment()
                + "\n\n💡 **Suggested fix:**\n```" + codeFence + "\n"
                + suggestion.trim() + "\n```";
    }

    private record ReviewResult(int filesReviewed, int commentsGenerated) {}
}
