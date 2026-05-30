package com.aireviewer.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.aireviewer.model.ProgressStage;
import com.aireviewer.model.PullRequestEvent;
import com.aireviewer.model.ReviewProgressEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ReviewProgressPublisherTest {

    private static final Instant NOW = Instant.parse("2026-05-30T00:00:00Z");

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Captor private ArgumentCaptor<ReviewProgressEvent> captor;

    private ReviewProgressPublisher publisher;

    private ReviewProgressPublisher publisher() {
        return new ReviewProgressPublisher(messagingTemplate, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static PullRequestEvent event() {
        return new PullRequestEvent(7, "octo/repo", "octo", "headsha", "basesha",
                "url", "opened", "octo", "d-1", NOW);
    }

    @Test
    void started_sends_started_stage_to_progress_topic() {
        publisher = publisher();

        publisher.started(event(), 3);

        verify(messagingTemplate).convertAndSend(eq(ReviewProgressPublisher.DESTINATION), captor.capture());
        ReviewProgressEvent sent = captor.getValue();
        assertThat(sent.stage()).isEqualTo(ProgressStage.STARTED);
        assertThat(sent.deliveryId()).isEqualTo("d-1");
        assertThat(sent.filesTotal()).isEqualTo(3);
        assertThat(sent.filesDone()).isZero();
        assertThat(sent.timestamp()).isEqualTo(NOW);
    }

    @Test
    void file_reviewed_carries_filename_and_counters() {
        publisher = publisher();

        publisher.fileReviewed(event(), "A.java", 3, 1);

        verify(messagingTemplate).convertAndSend(eq(ReviewProgressPublisher.DESTINATION), captor.capture());
        ReviewProgressEvent sent = captor.getValue();
        assertThat(sent.stage()).isEqualTo(ProgressStage.FILE_REVIEWED);
        assertThat(sent.filename()).isEqualTo("A.java");
        assertThat(sent.filesDone()).isEqualTo(1);
    }

    @Test
    void completed_sends_completed_stage_with_message() {
        publisher = publisher();

        publisher.completed(event(), 3, 3, "done");

        verify(messagingTemplate).convertAndSend(eq(ReviewProgressPublisher.DESTINATION), captor.capture());
        assertThat(captor.getValue().stage()).isEqualTo(ProgressStage.COMPLETED);
        assertThat(captor.getValue().message()).isEqualTo("done");
    }
}
