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
 * the rest of the review. Comment posting and live progress are layered on in the
 * following phases.
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

    @Override
    public void process(PullRequestEvent event) {
        String repoName = repoName(event.repoFullName());
        List<FileDiff> files = gitHubApiClient.fetchChangedFiles(
                event.repoOwner(), repoName, event.prNumber());
        log.info("Reviewing {} changed file(s) for {}#{}",
                files.size(), event.repoFullName(), event.prNumber());

        List<ReviewedFile> reviewed = new ArrayList<>();
        for (FileDiff file : files) {
            reviewFile(event, file).ifPresent(reviewed::add);
        }
        publishReview(event, repoName, reviewed);
    }

    private Optional<ReviewedFile> reviewFile(PullRequestEvent event, FileDiff file) {
        if (!diffParserService.isReviewable(file)) {
            return Optional.empty();
        }
        if (cacheCheckService.isAlreadyReviewed(event.repoFullName(), file)) {
            auditLogService.recordSkipped(event, file);
            return Optional.empty();
        }

        Optional<ReviewFeedback> feedback = llmReviewService.review(file.filename(), file.patch());
        if (feedback.isEmpty()) {
            auditLogService.recordFileFailure(event, file);
            return Optional.empty();
        }

        auditLogService.recordReviewed(event, file, feedback.get());
        cacheCheckService.markReviewed(event.repoFullName(), file);
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
