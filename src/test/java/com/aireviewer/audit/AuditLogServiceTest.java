package com.aireviewer.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.aireviewer.model.FileDiff;
import com.aireviewer.model.PullRequestEvent;
import com.aireviewer.model.ReviewAuditLog;
import com.aireviewer.model.ReviewFeedback;
import com.aireviewer.model.ReviewIssue;
import com.aireviewer.model.ReviewStatus;
import com.aireviewer.model.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    private static final Instant FIXED = Instant.parse("2026-05-30T08:00:00Z");

    @Mock private ReviewAuditLogRepository repository;

    private AuditLogService service;

    @BeforeEach
    void setUp() {
        when(repository.save(any(ReviewAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new AuditLogService(repository, new ObjectMapper(), Clock.fixed(FIXED, ZoneOffset.UTC));
    }

    private static PullRequestEvent event() {
        return new PullRequestEvent(7, "octo/repo", "octo", "headsha", "basesha",
                "url", "opened", "octo", "d-1", FIXED);
    }

    private static FileDiff file() {
        return new FileDiff("src/Main.java", "modified", "@@ patch @@", "sha", 3, 1);
    }

    private ReviewAuditLog capture() {
        ArgumentCaptor<ReviewAuditLog> captor = ArgumentCaptor.forClass(ReviewAuditLog.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordReviewed_persists_feedback_counts_and_critical_flag() {
        ReviewFeedback fb = new ReviewFeedback("did x", List.of(
                new ReviewIssue(10, Severity.CRITICAL, "bug"),
                new ReviewIssue(null, Severity.SUGGESTION, "nit")), false);

        service.recordReviewed(event(), file(), fb);

        ReviewAuditLog saved = capture();
        assertThat(saved.getStatus()).isEqualTo(ReviewStatus.REVIEWED);
        assertThat(saved.getFilePath()).isEqualTo("src/Main.java");
        assertThat(saved.getIssuesFound()).isEqualTo(2);
        assertThat(saved.isHasCritical()).isTrue();
        assertThat(saved.getLlmFeedback()).contains("did x").contains("CRITICAL");
        assertThat(saved.getCreatedAt()).isEqualTo(FIXED);
    }

    @Test
    void recordSkipped_persists_skipped_status_with_no_feedback() {
        service.recordSkipped(event(), file());

        ReviewAuditLog saved = capture();
        assertThat(saved.getStatus()).isEqualTo(ReviewStatus.SKIPPED);
        assertThat(saved.getLlmFeedback()).isNull();
        assertThat(saved.getIssuesFound()).isZero();
    }

    @Test
    void recordFileFailure_persists_failed_status_for_the_file() {
        service.recordFileFailure(event(), file());

        ReviewAuditLog saved = capture();
        assertThat(saved.getStatus()).isEqualTo(ReviewStatus.FAILED);
        assertThat(saved.getFilePath()).isEqualTo("src/Main.java");
    }

    @Test
    void recordFailure_persists_pipeline_level_failed_entry() {
        service.recordFailure(event());

        ReviewAuditLog saved = capture();
        assertThat(saved.getStatus()).isEqualTo(ReviewStatus.FAILED);
        assertThat(saved.getFilePath()).isEqualTo(ReviewAuditLog.PIPELINE_LEVEL);
    }
}
