package com.aireviewer.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.aireviewer.model.PullRequestEvent;
import com.aireviewer.model.ReviewAuditLog;
import com.aireviewer.model.ReviewStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    private static final Instant FIXED = Instant.parse("2026-05-29T08:00:00Z");

    @Mock private ReviewAuditLogRepository repository;

    @Test
    void recordFailure_persists_a_pipeline_level_failed_entry() {
        when(repository.save(any(ReviewAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        AuditLogService service = new AuditLogService(repository, Clock.fixed(FIXED, ZoneOffset.UTC));

        PullRequestEvent event = new PullRequestEvent(7, "octo/repo", "octo",
                "headsha", "basesha", "url", "opened", "octo", "d-1", FIXED);

        service.recordFailure(event);

        ArgumentCaptor<ReviewAuditLog> captor = ArgumentCaptor.forClass(ReviewAuditLog.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        ReviewAuditLog saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(ReviewStatus.FAILED);
        assertThat(saved.getFilePath()).isEqualTo(ReviewAuditLog.PIPELINE_LEVEL);
        assertThat(saved.getRepoFullName()).isEqualTo("octo/repo");
        assertThat(saved.getPrNumber()).isEqualTo(7);
        assertThat(saved.getCommitSha()).isEqualTo("headsha");
        assertThat(saved.getDeliveryId()).isEqualTo("d-1");
        assertThat(saved.isHasCritical()).isFalse();
        assertThat(saved.getIssuesFound()).isZero();
        assertThat(saved.getCreatedAt()).isEqualTo(FIXED);
    }
}
