package com.aireviewer.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.aireviewer.model.PullRequestEvent;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.junit.jupiter.api.Test;

class PullRequestEventSerializationTest {

    @Test
    void serializes_event_to_json_with_all_fields() {
        PullRequestEvent event = new PullRequestEvent(7, "octo/repo", "octo",
                "headsha", "basesha", "https://x/42.diff", "opened", "octo", "d-1",
                Instant.parse("2026-05-29T00:00:00Z"));

        try (JsonSerializer<PullRequestEvent> serializer = new JsonSerializer<>()) {
            byte[] bytes = serializer.serialize(KafkaTopics.REVIEW_REQUESTED, event);
            String json = new String(bytes, StandardCharsets.UTF_8);

            assertThat(json)
                    .contains("\"prNumber\":7")
                    .contains("\"repoFullName\":\"octo/repo\"")
                    .contains("\"headSha\":\"headsha\"")
                    .contains("\"action\":\"opened\"")
                    .contains("\"deliveryId\":\"d-1\"");
        }
    }
}
