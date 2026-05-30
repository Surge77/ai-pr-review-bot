package com.aireviewer.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import com.aireviewer.model.AuditStatsResponse;
import com.aireviewer.model.ReviewAuditLog;
import com.aireviewer.model.ReviewStatus;
import com.aireviewer.model.ReviewSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

    private static final String REPO = "octo/repo";

    @Mock private ReviewAuditLogRepository repository;

    private AuditQueryService service;

    @BeforeEach
    void setUp() {
        service = new AuditQueryService(repository);
    }

    private static ReviewAuditLog row() {
        return ReviewAuditLog.builder()
                .id(1L).prNumber(7).repoFullName(REPO).filePath("A.java")
                .status(ReviewStatus.REVIEWED).issuesFound(2).hasCritical(true)
                .commitSha("sha").createdAt(Instant.parse("2026-05-30T00:00:00Z"))
                .build();
    }

    @Test
    @DisplayName("listReviews without a PR maps entities to projections, newest-first")
    void list_all_prs() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findByRepoFullNameOrderByCreatedAtDesc(REPO, pageable))
                .thenReturn(new PageImpl<>(List.of(row())));

        Page<ReviewSummaryResponse> page = service.listReviews(REPO, null, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().filePath()).isEqualTo("A.java");
        assertThat(page.getContent().getFirst().status()).isEqualTo(ReviewStatus.REVIEWED);
    }

    @Test
    @DisplayName("listReviews with a PR uses the PR-scoped query")
    void list_by_pr() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findByRepoFullNameAndPrNumberOrderByCreatedAtDesc(REPO, 7, pageable))
                .thenReturn(new PageImpl<>(List.of(row())));

        Page<ReviewSummaryResponse> page = service.listReviews(REPO, 7, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().prNumber()).isEqualTo(7);
    }

    @Test
    @DisplayName("stats aggregates counts and computes skip rate")
    void stats_computes_skip_rate() {
        when(repository.countByRepoFullName(REPO)).thenReturn(10L);
        when(repository.countByRepoFullNameAndStatus(REPO, ReviewStatus.REVIEWED)).thenReturn(5L);
        when(repository.countByRepoFullNameAndStatus(REPO, ReviewStatus.SKIPPED)).thenReturn(4L);
        when(repository.countByRepoFullNameAndStatus(REPO, ReviewStatus.FAILED)).thenReturn(1L);
        when(repository.sumIssuesFound(REPO)).thenReturn(12L);
        when(repository.countByRepoFullNameAndHasCriticalTrue(REPO)).thenReturn(3L);

        AuditStatsResponse stats = service.stats(REPO);

        assertThat(stats.total()).isEqualTo(10);
        assertThat(stats.skipped()).isEqualTo(4);
        assertThat(stats.totalIssues()).isEqualTo(12);
        assertThat(stats.criticalCount()).isEqualTo(3);
        assertThat(stats.skipRate()).isEqualTo(0.4);
    }

    @Test
    @DisplayName("stats skip rate is zero when there are no rows")
    void stats_zero_when_empty() {
        when(repository.countByRepoFullName(REPO)).thenReturn(0L);
        when(repository.sumIssuesFound(REPO)).thenReturn(0L);

        AuditStatsResponse stats = service.stats(REPO);

        assertThat(stats.total()).isZero();
        assertThat(stats.skipRate()).isZero();
    }
}
