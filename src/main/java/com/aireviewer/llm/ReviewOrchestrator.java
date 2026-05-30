package com.aireviewer.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.aireviewer.audit.AuditLogService;
import com.aireviewer.cache.CacheCheckService;
import com.aireviewer.config.GitHubProperties;
import com.aireviewer.diff.DiffParserService;
import com.aireviewer.github.GitHubApiClient;
import com.aireviewer.github.ReviewCommentAssembler;
import com.aireviewer.kafka.ReviewProcessor;
import com.aireviewer.model.FileDiff;
import com.aireviewer.model.PrReview;
import com.aireviewer.model.PullRequestEvent;
import com.aireviewer.model.ReviewFeedback;
import com.aireviewer.model.ReviewedFile;
import com.aireviewer.websocket.ReviewProgressPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Real {@link ReviewProcessor}: orchestrates the per-file review pipeline for a
 * pull-request event — fetch changed files, skip cached/binary files, run the LLM
 * review, and audit each outcome. Marked {@link Primary} so it supersedes the
 * placeholder processor.
 *
 * <p>Per-file failures are isolated (audited FAILED) so one bad file never aborts
 * the rest of the review. Each outcome is streamed to the dashboard, and the
 * accumulated feedback is posted back to the PR as one consolidated review.
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ReviewOrchestrator implements ReviewProcessor {

    private final GitHubApiClient gitHubApiClient;
    private final DiffParserService diffParserService;
    private final CacheCheckService cacheCheckService;
    private final LLMReviewService llmReviewService;
    private final AuditLogService auditLogService;
    private final ReviewCommentAssembler reviewCommentAssembler;
    private final GitHubProperties gitHubProperties;
    private final ReviewProgressPublisher progressPublisher;

    @Override
    public void process(PullRequestEvent event) {
        String repoName = repoName(event.repoFullName());
        List<FileDiff> files = gitHubApiClient.fetchChangedFiles(
                event.repoOwner(), repoName, event.prNumber());
        int total = files.size();
        log.info("Reviewing {} changed file(s) for {}#{}",
                total, event.repoFullName(), event.prNumber());
        progressPublisher.started(event, total);

        List<ReviewedFile> reviewed = new ArrayList<>();
        int done = 0;
        for (FileDiff file : files) {
            done++;
            reviewFile(event, file, total, done).ifPresent(reviewed::add);
        }
        publishReview(event, repoName, reviewed);
        progressPublisher.completed(event, total, done,
                "Review complete — " + reviewed.size() + " file(s) reviewed");
    }

    private Optional<ReviewedFile> reviewFile(PullRequestEvent event, FileDiff file, int total, int done) {
        if (!diffParserService.isReviewable(file)) {
            return Optional.empty();
        }
        if (cacheCheckService.isAlreadyReviewed(event.repoFullName(), file)) {
            auditLogService.recordSkipped(event, file);
            progressPublisher.fileSkipped(event, file.filename(), total, done);
            return Optional.empty();
        }

        Optional<ReviewFeedback> feedback = llmReviewService.review(file.filename(), file.patch());
        if (feedback.isEmpty()) {
            auditLogService.recordFileFailure(event, file);
            progressPublisher.fileFailed(event, file.filename(), total, done);
            return Optional.empty();
        }

        auditLogService.recordReviewed(event, file, feedback.get());
        cacheCheckService.markReviewed(event.repoFullName(), file);
        progressPublisher.fileReviewed(event, file.filename(), total, done);
        return Optional.of(new ReviewedFile(file, feedback.get()));
    }

    private void publishReview(PullRequestEvent event, String repoName, List<ReviewedFile> reviewed) {
        if (reviewed.isEmpty()) {
            return;
        }
        PrReview review = reviewCommentAssembler.assemble(reviewed);
        if (!gitHubProperties.postComments()) {
            log.info("Comment posting disabled (dry-run); skipping review for {}#{}",
                    event.repoFullName(), event.prNumber());
            return;
        }
        gitHubApiClient.postReview(event.repoOwner(), repoName, event.prNumber(),
                event.headSha(), review);
    }

    private String repoName(String repoFullName) {
        int slash = repoFullName.indexOf('/');
        return slash >= 0 ? repoFullName.substring(slash + 1) : repoFullName;
    }
}
