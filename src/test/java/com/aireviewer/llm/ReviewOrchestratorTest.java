package com.aireviewer.llm;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.aireviewer.audit.AuditLogService;
import com.aireviewer.cache.CacheCheckService;
import com.aireviewer.diff.DiffParserService;
import com.aireviewer.github.GitHubApiClient;
import com.aireviewer.model.FileDiff;
import com.aireviewer.model.PullRequestEvent;
import com.aireviewer.model.ReviewFeedback;
import org.junit.jupiter.api.BeforeEach;
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

    private ReviewOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new ReviewOrchestrator(gitHubApiClient, diffParserService,
                cacheCheckService, llmReviewService, auditLogService);
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

        orchestrator.process(event());

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

        orchestrator.process(event());

        verify(auditLogService).recordReviewed(eq(event()), eq(f), eq(fb));
        verify(cacheCheckService).markReviewed("octo/repo", f);
    }

    @Test
    void cache_hit_records_skipped_and_does_not_call_llm() {
        FileDiff f = file("B.java");
        when(gitHubApiClient.fetchChangedFiles("octo", "repo", 7)).thenReturn(List.of(f));
        when(diffParserService.isReviewable(f)).thenReturn(true);
        when(cacheCheckService.isAlreadyReviewed("octo/repo", f)).thenReturn(true);

        orchestrator.process(event());

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

        orchestrator.process(event());

        verify(auditLogService).recordFileFailure(eq(event()), eq(f));
        verify(cacheCheckService, never()).markReviewed("octo/repo", f);
    }

    @Test
    void non_reviewable_file_is_skipped_entirely() {
        FileDiff binary = file("img.png");
        when(gitHubApiClient.fetchChangedFiles("octo", "repo", 7)).thenReturn(List.of(binary));
        when(diffParserService.isReviewable(binary)).thenReturn(false);

        orchestrator.process(event());

        verifyNoInteractions(llmReviewService, cacheCheckService, auditLogService);
    }
}
