package com.aireviewer.websocket;

import java.time.Clock;

import com.aireviewer.model.ProgressStage;
import com.aireviewer.model.PullRequestEvent;
import com.aireviewer.model.ReviewProgressEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Broadcasts {@link ReviewProgressEvent}s to the dashboard over STOMP. Each
 * helper builds a stage-specific event and sends it to the shared progress
 * topic; clients filter by {@code deliveryId}.
 */
@Component
@RequiredArgsConstructor
public class ReviewProgressPublisher {

    static final String DESTINATION = "/topic/progress";

    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    /** PR review started; reports the total number of changed files. */
    public void started(PullRequestEvent event, int filesTotal) {
        send(event, ProgressStage.STARTED, null, filesTotal, 0,
                "Review started for " + filesTotal + " file(s)");
    }

    /** A file was reviewed by the LLM. */
    public void fileReviewed(PullRequestEvent event, String filename, int filesTotal, int filesDone) {
        send(event, ProgressStage.FILE_REVIEWED, filename, filesTotal, filesDone, "Reviewed " + filename);
    }

    /** A file was skipped (cached, unchanged diff). */
    public void fileSkipped(PullRequestEvent event, String filename, int filesTotal, int filesDone) {
        send(event, ProgressStage.FILE_SKIPPED, filename, filesTotal, filesDone, "Skipped " + filename);
    }

    /** A file failed to be reviewed. */
    public void fileFailed(PullRequestEvent event, String filename, int filesTotal, int filesDone) {
        send(event, ProgressStage.FILE_FAILED, filename, filesTotal, filesDone, "Failed " + filename);
    }

    /** PR review completed. */
    public void completed(PullRequestEvent event, int filesTotal, int filesDone, String message) {
        send(event, ProgressStage.COMPLETED, null, filesTotal, filesDone, message);
    }

    private void send(PullRequestEvent event, ProgressStage stage, String filename,
                      int filesTotal, int filesDone, String message) {
        ReviewProgressEvent progress = new ReviewProgressEvent(
                event.deliveryId(), event.repoFullName(), event.prNumber(),
                stage, filename, filesTotal, filesDone, message, clock.instant());
        messagingTemplate.convertAndSend(DESTINATION, progress);
    }
}
