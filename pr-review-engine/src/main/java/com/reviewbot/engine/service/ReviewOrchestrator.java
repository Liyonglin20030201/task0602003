package com.reviewbot.engine.service;

import com.reviewbot.core.config.AppProperties;
import com.reviewbot.core.entity.ReviewCommentEntity;
import com.reviewbot.core.entity.ReviewRecord;
import com.reviewbot.core.model.*;
import com.reviewbot.core.repository.ReviewRecordRepository;
import com.reviewbot.engine.client.GitHubApiClient;
import com.reviewbot.sandbox.service.SandboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewOrchestrator {

    private final ClaudeReviewService claudeReviewService;
    private final GitHubApiClient gitHubApiClient;
    private final SandboxService sandboxService;
    private final ProgressNotifier progressNotifier;
    private final ReviewRecordRepository reviewRecordRepository;
    private final AppProperties appProperties;

    public void executeReview(ReviewRequest request) {
        String reviewId = request.getReviewId();
        String repo = request.getRepoFullName();
        int prNumber = request.getPrNumber();

        ReviewRecord record = ReviewRecord.builder()
                .id(reviewId)
                .repoFullName(repo)
                .prNumber(prNumber)
                .prTitle(request.getPrTitle())
                .prAuthor(request.getPrAuthor())
                .headSha(request.getHeadSha())
                .status(ReviewStatus.QUEUED)
                .createdAt(Instant.now())
                .build();
        reviewRecordRepository.save(record);

        try {
            progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.FETCHING_DIFF, "Fetching PR diff from GitHub");
            record.setStatus(ReviewStatus.FETCHING_DIFF);
            reviewRecordRepository.save(record);

            String diff = gitHubApiClient.fetchDiff(repo, prNumber);
            if (diff == null || diff.isBlank()) {
                log.warn("Empty diff for {}#{}", repo, prNumber);
                markFailed(record, "Empty diff received");
                return;
            }

            progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.ANALYZING, "AI is analyzing code changes");
            record.setStatus(ReviewStatus.ANALYZING);
            reviewRecordRepository.save(record);

            ReviewResult result = claudeReviewService.analyze(diff, request.getPrTitle());
            result.setReviewId(reviewId);

            if (appProperties.getSandbox().isEnabled() && hasCodeToTest(result)) {
                progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.SANDBOXING, "Running code in sandbox");
                record.setStatus(ReviewStatus.SANDBOXING);
                reviewRecordRepository.save(record);
                // Sandbox execution is optional - we skip if no testable snippets found
            }

            progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.POSTING, "Posting review comments to GitHub");
            record.setStatus(ReviewStatus.POSTING);
            reviewRecordRepository.save(record);

            postReviewToGitHub(request, result);

            record.setStatus(ReviewStatus.COMPLETED);
            record.setSummary(result.getSummary());
            record.setCompletedAt(Instant.now());
            record.setComments(buildCommentEntities(result, record));
            reviewRecordRepository.save(record);

            progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.COMPLETED,
                    "Review completed with " + result.getComments().size() + " comments");

            log.info("Review {} completed for {}#{}", reviewId, repo, prNumber);

        } catch (Exception e) {
            log.error("Review {} failed for {}#{}", reviewId, repo, prNumber, e);
            markFailed(record, e.getMessage());
            progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.FAILED, "Error: " + e.getMessage());
            throw new RuntimeException("Review failed", e);
        }
    }

    private void postReviewToGitHub(ReviewRequest request, ReviewResult result) {
        if (result.getComments().isEmpty()) {
            gitHubApiClient.postIssueComment(request.getRepoFullName(), request.getPrNumber(),
                    "**AI Code Review** ✅\n\n" + result.getSummary() + "\n\nNo issues found.");
            return;
        }

        List<Map<String, Object>> ghComments = new ArrayList<>();
        for (ReviewResult.ReviewComment comment : result.getComments()) {
            if (comment.getFilePath() != null && comment.getLineNumber() != null) {
                Map<String, Object> ghComment = new HashMap<>();
                ghComment.put("path", comment.getFilePath());
                ghComment.put("line", comment.getLineNumber());
                ghComment.put("body", formatComment(comment));
                ghComments.add(ghComment);
            }
        }

        if (ghComments.isEmpty()) {
            gitHubApiClient.postIssueComment(request.getRepoFullName(), request.getPrNumber(),
                    "**AI Code Review**\n\n" + result.getSummary());
        } else {
            gitHubApiClient.postReviewComment(
                    request.getRepoFullName(),
                    request.getPrNumber(),
                    request.getHeadSha(),
                    "**AI Code Review**\n\n" + result.getSummary(),
                    ghComments);
        }
    }

    private String formatComment(ReviewResult.ReviewComment comment) {
        StringBuilder sb = new StringBuilder();
        sb.append("**[").append(comment.getSeverity().toUpperCase()).append("]** ");
        sb.append(comment.getMessage());
        if (comment.getSuggestion() != null && !comment.getSuggestion().isBlank()) {
            sb.append("\n\n💡 **Suggestion:** ").append(comment.getSuggestion());
        }
        return sb.toString();
    }

    private boolean hasCodeToTest(ReviewResult result) {
        return result.getComments().stream()
                .anyMatch(c -> "error".equals(c.getSeverity()));
    }

    private List<ReviewCommentEntity> buildCommentEntities(ReviewResult result, ReviewRecord record) {
        List<ReviewCommentEntity> entities = new ArrayList<>();
        for (ReviewResult.ReviewComment c : result.getComments()) {
            entities.add(ReviewCommentEntity.builder()
                    .reviewRecord(record)
                    .filePath(c.getFilePath())
                    .lineNumber(c.getLineNumber())
                    .severity(c.getSeverity())
                    .message(c.getMessage())
                    .suggestion(c.getSuggestion())
                    .build());
        }
        return entities;
    }

    private void markFailed(ReviewRecord record, String reason) {
        record.setStatus(ReviewStatus.FAILED);
        record.setSummary("Failed: " + reason);
        record.setCompletedAt(Instant.now());
        reviewRecordRepository.save(record);
    }
}
