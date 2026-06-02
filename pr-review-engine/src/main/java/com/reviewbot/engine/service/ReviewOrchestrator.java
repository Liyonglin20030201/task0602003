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

import java.nio.file.Path;
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
    private final RepositoryCloneService repositoryCloneService;
    private final CodeSnippetExtractor codeSnippetExtractor;
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
            // Step 1: Clone/pull the full repository
            progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.CLONING_REPO, "Cloning repository to local");
            record.setStatus(ReviewStatus.CLONING_REPO);
            reviewRecordRepository.save(record);

            Path repoDir = repositoryCloneService.cloneOrPull(repo, request.getHeadSha());

            // Step 2: Fetch the diff
            progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.FETCHING_DIFF, "Fetching PR diff from GitHub");
            record.setStatus(ReviewStatus.FETCHING_DIFF);
            reviewRecordRepository.save(record);

            String diff = gitHubApiClient.fetchDiff(repo, prNumber);
            if (diff == null || diff.isBlank()) {
                log.warn("Empty diff for {}#{}", repo, prNumber);
                markFailed(record, "Empty diff received");
                return;
            }

            // Step 3: AI analysis
            progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.ANALYZING, "AI is analyzing code changes");
            record.setStatus(ReviewStatus.ANALYZING);
            reviewRecordRepository.save(record);

            ReviewResult result = claudeReviewService.analyze(diff, request.getPrTitle());
            result.setReviewId(reviewId);

            // Step 4: Extract testable snippets and run in sandbox
            if (appProperties.getSandbox().isEnabled()) {
                progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.SANDBOXING, "Extracting and running code snippets in sandbox");
                record.setStatus(ReviewStatus.SANDBOXING);
                reviewRecordRepository.save(record);

                List<CodeSnippetExtractor.TestableSnippet> snippets = codeSnippetExtractor.extractTestableSnippets(diff, repoDir);
                List<ReviewResult.SnippetExecutionResult> sandboxResults = executeSnippetsInSandbox(snippets, reviewId, repo, prNumber);
                result.setSandboxResults(sandboxResults);

                log.info("Sandbox completed: {} snippets executed for review {}", sandboxResults.size(), reviewId);
            }

            // Step 5: Post results to GitHub
            progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.POSTING, "Posting review comments to GitHub");
            record.setStatus(ReviewStatus.POSTING);
            reviewRecordRepository.save(record);

            postReviewToGitHub(request, result);

            record.setStatus(ReviewStatus.COMPLETED);
            record.setSummary(result.getSummary());
            record.setCompletedAt(Instant.now());
            record.setComments(buildCommentEntities(result, record));
            reviewRecordRepository.save(record);

            int sandboxCount = result.getSandboxResults() != null ? result.getSandboxResults().size() : 0;
            progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.COMPLETED,
                    "Review completed with " + result.getComments().size() + " comments, " + sandboxCount + " snippets tested");

            log.info("Review {} completed for {}#{}", reviewId, repo, prNumber);

        } catch (Exception e) {
            log.error("Review {} failed for {}#{}", reviewId, repo, prNumber, e);
            markFailed(record, e.getMessage());
            progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.FAILED, "Error: " + e.getMessage());
            throw new RuntimeException("Review failed", e);
        }
    }

    private List<ReviewResult.SnippetExecutionResult> executeSnippetsInSandbox(
            List<CodeSnippetExtractor.TestableSnippet> snippets, String reviewId, String repo, int prNumber) {

        List<ReviewResult.SnippetExecutionResult> results = new ArrayList<>();

        for (int i = 0; i < snippets.size(); i++) {
            CodeSnippetExtractor.TestableSnippet snippet = snippets.get(i);
            progressNotifier.notify(reviewId, repo, prNumber, ReviewStatus.SANDBOXING,
                    String.format("Running snippet %d/%d: %s", i + 1, snippets.size(), snippet.getClassName()));

            try {
                SandboxResult sandboxResult = sandboxService.execute(snippet.getSourceCode(), snippet.getClassName());
                results.add(ReviewResult.SnippetExecutionResult.builder()
                        .filePath(snippet.getFilePath())
                        .className(snippet.getClassName())
                        .sandboxResult(sandboxResult)
                        .build());

                log.info("Sandbox result for {}: exitCode={}, timedOut={}",
                        snippet.getClassName(), sandboxResult.getExitCode(), sandboxResult.isTimedOut());
            } catch (Exception e) {
                log.warn("Sandbox execution failed for {}: {}", snippet.getClassName(), e.getMessage());
                results.add(ReviewResult.SnippetExecutionResult.builder()
                        .filePath(snippet.getFilePath())
                        .className(snippet.getClassName())
                        .sandboxResult(SandboxResult.builder()
                                .exitCode(-1)
                                .stdout("")
                                .stderr("Sandbox error: " + e.getMessage())
                                .timedOut(false)
                                .executionTimeMs(0)
                                .build())
                        .build());
            }
        }
        return results;
    }

    private void postReviewToGitHub(ReviewRequest request, ReviewResult result) {
        String body = buildReviewBody(result);

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
            gitHubApiClient.postIssueComment(request.getRepoFullName(), request.getPrNumber(), body);
        } else {
            gitHubApiClient.postReviewComment(
                    request.getRepoFullName(),
                    request.getPrNumber(),
                    request.getHeadSha(),
                    body,
                    ghComments);
        }
    }

    private String buildReviewBody(ReviewResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("**AI Code Review**\n\n");
        sb.append(result.getSummary()).append("\n");

        if (result.getSandboxResults() != null && !result.getSandboxResults().isEmpty()) {
            sb.append("\n---\n\n");
            sb.append("**Sandbox Execution Results**\n\n");

            for (ReviewResult.SnippetExecutionResult exec : result.getSandboxResults()) {
                SandboxResult sr = exec.getSandboxResult();
                String statusIcon = sr.getExitCode() == 0 ? "pass" : "FAIL";
                sb.append(String.format("| `%s` | %s | %dms |\n",
                        exec.getFilePath(), statusIcon, sr.getExecutionTimeMs()));

                if (sr.isTimedOut()) {
                    sb.append("  - Timed out\n");
                } else if (sr.getExitCode() != 0) {
                    sb.append("  - Exit code: ").append(sr.getExitCode()).append("\n");
                    if (sr.getStderr() != null && !sr.getStderr().isBlank()) {
                        String stderr = sr.getStderr().length() > 500
                                ? sr.getStderr().substring(0, 500) + "..."
                                : sr.getStderr();
                        sb.append("  ```\n  ").append(stderr).append("\n  ```\n");
                    }
                }
            }

            long passed = result.getSandboxResults().stream()
                    .filter(r -> r.getSandboxResult().getExitCode() == 0)
                    .count();
            long total = result.getSandboxResults().size();
            sb.append(String.format("\n**Summary:** %d/%d snippets compiled and ran successfully.\n", passed, total));
        }

        if (result.getComments().isEmpty() && (result.getSandboxResults() == null || result.getSandboxResults().isEmpty())) {
            sb.append("\nNo issues found.");
        }

        return sb.toString();
    }

    private String formatComment(ReviewResult.ReviewComment comment) {
        StringBuilder sb = new StringBuilder();
        sb.append("**[").append(comment.getSeverity().toUpperCase()).append("]** ");
        sb.append(comment.getMessage());
        if (comment.getSuggestion() != null && !comment.getSuggestion().isBlank()) {
            sb.append("\n\n**Suggestion:** ").append(comment.getSuggestion());
        }
        return sb.toString();
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

        // Add sandbox failure comments
        if (result.getSandboxResults() != null) {
            for (ReviewResult.SnippetExecutionResult exec : result.getSandboxResults()) {
                if (exec.getSandboxResult().getExitCode() != 0) {
                    String message = exec.getSandboxResult().isTimedOut()
                            ? "Code execution timed out"
                            : "Code failed to compile or run (exit code: " + exec.getSandboxResult().getExitCode() + ")";
                    String detail = exec.getSandboxResult().getStderr();
                    entities.add(ReviewCommentEntity.builder()
                            .reviewRecord(record)
                            .filePath(exec.getFilePath())
                            .lineNumber(1)
                            .severity("error")
                            .message(message)
                            .suggestion(detail != null && !detail.isBlank() ? detail : null)
                            .build());
                }
            }
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
