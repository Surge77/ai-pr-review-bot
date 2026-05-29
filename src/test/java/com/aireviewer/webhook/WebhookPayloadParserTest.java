package com.aireviewer.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.aireviewer.model.PullRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class WebhookPayloadParserTest {

    private static final Instant FIXED = Instant.parse("2026-05-29T12:00:00Z");

    private final WebhookPayloadParser parser =
            new WebhookPayloadParser(new ObjectMapper(), Clock.fixed(FIXED, ZoneOffset.UTC));

    private static final String PAYLOAD = """
            {
              "action": "opened",
              "number": 42,
              "pull_request": {
                "number": 42,
                "head": { "sha": "headsha123" },
                "base": { "sha": "basesha456" },
                "diff_url": "https://github.com/octocat/hello-world/pull/42.diff"
              },
              "repository": { "full_name": "octocat/hello-world", "owner": { "login": "octocat" } },
              "sender": { "login": "octocat" }
            }
            """;

    @Test
    void maps_all_fields_from_a_pull_request_payload() throws Exception {
        PullRequestEvent event = parser.parse(PAYLOAD.getBytes(StandardCharsets.UTF_8), "delivery-1");

        assertThat(event.prNumber()).isEqualTo(42);
        assertThat(event.repoFullName()).isEqualTo("octocat/hello-world");
        assertThat(event.repoOwner()).isEqualTo("octocat");
        assertThat(event.headSha()).isEqualTo("headsha123");
        assertThat(event.baseSha()).isEqualTo("basesha456");
        assertThat(event.diffUrl()).isEqualTo("https://github.com/octocat/hello-world/pull/42.diff");
        assertThat(event.action()).isEqualTo("opened");
        assertThat(event.senderLogin()).isEqualTo("octocat");
        assertThat(event.deliveryId()).isEqualTo("delivery-1");
        assertThat(event.receivedAt()).isEqualTo(FIXED);
        assertThat(event.partitionKey()).isEqualTo("octocat/hello-world#42");
    }

    @Test
    void throws_on_malformed_json() {
        byte[] garbage = "{ not json".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> parser.parse(garbage, "delivery-2"))
                .isInstanceOf(java.io.IOException.class);
    }
}
