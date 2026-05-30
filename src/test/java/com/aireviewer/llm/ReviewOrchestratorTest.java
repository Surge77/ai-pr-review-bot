package com.aireviewer.llm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.aireviewer.audit.AuditLogService;
import com.aireviewer.cache.CacheCheckService;
import com.aireviewer.config.GitHubProperties;
import com.aireviewer.diff.DiffParserService;
import com.aireviewer.github.GitHubApiClient;
import com.aireviewer.github.ReviewCommentAssembler;
import com.aireviewer.model.FileDiff;
import com.aireviewer.model.PrReview;
import com.aireviewer.model.PullRequestEvent;
import com.aireviewer.model.ReviewFeedback;
import com.aireviewer.websocket.ReviewProgressPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewOrchestratorTest {

    @Mock private GitHubApiClient gitHubApiClient;
    @Mock private DiffParserService diffParserService;
    @Mock private CacheCheckService cacheCheckService;
    @Mock private LLMReviewService llmReviewService;
    @Mock private AuditLogService auditLogService;
    @Mock private ReviewCommentAssembler reviewCommentAssembler;
    @Mock private ReviewProgressPublisher progressPublisher;

    private static final PrReview ASSEMBLED = new PrReview("body", List.of());

    private ReviewOrchestrator orchestrator(boolean postComments) {
        lenient().when(reviewCommentAssembler.assemble(any())).thenReturn(ASSEMBLED);
        GitHubProperties props = new GitHubProperties("https://api.github.com", "tok", postComments);
        return new ReviewOrchestrator(gitHubApiClient, diffParserService, cacheCheckService,
                llmReviewService, auditLogService, reviewCommentAssembler, props, progressPublisher);
    }

    private static PullRequestEvent event() {
        return new PullRequestEvent(7, "octo/repo", "octo", "headsha", "basesha",
                "url", "opened", "octo", "d-1", Instant.parse("2026-05-30T00:00:00Z"));
    }

    private static FileDiff file(String name) {
        return new FileDiff(name, "modified", "@@ p @@", "sha", 1, 0);
    }

    @Test
    void splits_repo_full_name_and_fetches_files() {
        when(gitHubApiClient.fetchChangedFiles("octo", "repo", 7)).thenReturn(List.of());

        orchestrator(true).process(event());

        verify(gitHubApiClient).fetchChangedFiles("octo", "repo", 7);
    }

    @Test
    void reviews_a_cache_miss_then_audits_and_caches() {
        FileDiff f = file("A.java");
        ReviewFeedback fb = new ReviewFeedback("ok", List.of(), true);
        when(gitHubApiClient.fetchChangedFiles("octo", "repo", 7)).thenReturn(List.of(f));
        when(diffParserService.isReviewable(f)).thenReturn(true);
        when(cacheCheckService.isAlreadyReviewed("octo/repo", f)).thenReturn(false);
        when(llmReviewService.review("A.java", f.patch())).thenReturn(Optional.of(fb));

        orchestrator(true).process(event());

        verify(auditLogService).recordReviewed(eq(event()), eq(f), eq(fb));
        verify(cacheCheckService).markReviewed("octo/repo", f);
    }

    @Test
    void cache_hit_records_skipped_and_does_not_call_llm() {
        FileDiff f = file("B.java");
        when(gitHubApiClient.fetchChangedFiles("octo", "repo", 7)).thenReturn(List.of(f));
        when(diffParserService.isReviewable(f)).thenReturn(true);
        when(cacheCheckService.isAlreadyReviewed("octo/repo", f)).thenReturn(true);

        orchestrator(true).process(event());

        verify(auditLogService).recordSkipped(eq(event()), eq(f));
        verifyNoInteractions(llmReviewService);
    }

    @Test
    void llm_failure_records_file_failure() {
        FileDiff f = file("C.java");
        when(gitHubApiClient.fetchChangedFiles("octo", "repo", 7)).thenReturn(List.of(f));
        when(diffParserService.isReviewable(f)).thenReturn(true);
        when(cacheCheckService.isAlreadyReviewed("octo/repo", f)).thenReturn(false);
        when(llmReviewService.review("C.java", f.patch())).thenReturn(Optional.empty());

        orchestrator(true).process(event());

        verify(auditLogService).recordFileFailure(eq(event()), eq(f));
        verify(cacheCheckService, never()).markReviewed("octo/repo", f);
    }

    @Test
    void non_reviewable_file_is_skipped_entirely() {
        FileDiff binary = file("img.png");
        when(gitHubApiClient.fetchChangedFiles("octo", "repo", 7)).thenReturn(List.of(binary));
        when(diffParserService.isReviewable(binary)).thenReturn(false);

        orchestrator(true).process(event());

        verifyNoInteractions(llmReviewService, cacheCheckService, auditLogService);
        verify(gitHubApiClient, never()).postReview(any(), any(), eq(7), any(), any());
    }

    @Test
    void posts_one_consolidated_review_for_reviewed_files() {
        FileDiff f = file("A.java");
        ReviewFeedback fb = new ReviewFeedback("ok", List.of(), true);
        when(gitHubApiClient.fetchChangedFiles("octo", "repo", 7)).thenReturn(List.of(f));
        when(diffParserService.isReviewable(f)).thenReturn(true);
        when(cacheCheckService.isAlreadyReviewed("octo/repo", f)).thenReturn(false);
        when(llmReviewService.review("A.java", f.patch())).thenReturn(Optional.of(fb));

        orchestrator(true).process(event());

        verify(reviewCommentAssembler).assemble(any());
        verify(gitHubApiClient).postReview("octo", "repo", 7, "headsha", ASSEMBLED);
    }

    @Test
    void streams_progress_for_started_reviewed_and_completed() {
        FileDiff f = file("A.java");
        ReviewFeedback fb = new ReviewFeedback("ok", List.of(), true);
        when(gitHubApiClient.fetchChangedFiles("octo", "repo", 7)).thenReturn(List.of(f));
        when(diffParserService.isReviewable(f)).thenReturn(true);
        when(cacheCheckService.isAlreadyReviewed("octo/repo", f)).thenReturn(false);
        when(llmReviewService.review("A.java", f.patch())).thenReturn(Optional.of(fb));

        orchestrator(true).process(event());

        verify(progressPublisher).started(event(), 1);
        verify(progressPublisher).fileReviewed(event(), "A.java", 1, 1);
        verify(progressPublisher).completed(eq(event()), eq(1), eq(1), any());
    }

    @Test
    void dry_run_assembles_but_does_not_post() {
        FileDiff f = file("A.java");
        ReviewFeedback fb = new ReviewFeedback("ok", List.of(), true);
        when(gitHubApiClient.fetchChangedFiles("octo", "repo", 7)).thenReturn(List.of(f));
        when(diffParserService.isReviewable(f)).thenReturn(true);
        when(cacheCheckService.isAlreadyReviewed("octo/repo", f)).thenReturn(false);
        when(llmReviewService.review("A.java", f.patch())).thenReturn(Optional.of(fb));

        orchestrator(false).process(event());

        verify(gitHubApiClient, never()).postReview(any(), any(), eq(7), any(), any());
    }
}
