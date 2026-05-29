package com.aireviewer.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.aireviewer.config.WebhookProperties;
import com.aireviewer.kafka.PullRequestEventPublisher;
import com.aireviewer.model.PullRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    private static final byte[] BODY = "{}".getBytes(StandardCharsets.UTF_8);
    private static final String SIG = "sha256=whatever";

    @Mock private GitHubSignatureValidator signatureValidator;
    @Mock private WebhookPayloadParser payloadParser;
    @Mock private PullRequestEventPublisher eventPublisher;

    private WebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new WebhookController(signatureValidator, payloadParser, eventPublisher,
                new WebhookProperties("secret", List.of("opened", "synchronize")));
    }

    private static PullRequestEvent event(String action) {
        return new PullRequestEvent(7, "octo/repo", "octo", "h", "b", "url",
                action, "octo", "d-1", Instant.parse("2026-05-29T00:00:00Z"));
    }

    @Test
    void returns_401_and_does_not_publish_when_signature_is_invalid() {
        when(signatureValidator.isValid(BODY, SIG)).thenReturn(false);

        ResponseEntity<Map<String, String>> response =
                controller.receive(BODY, SIG, "pull_request", "d-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void ignores_non_pull_request_events() {
        when(signatureValidator.isValid(BODY, SIG)).thenReturn(true);

        ResponseEntity<Map<String, String>> response =
                controller.receive(BODY, SIG, "ping", "d-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ignored");
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void ignores_pull_request_actions_that_are_not_accepted() throws Exception {
        when(signatureValidator.isValid(BODY, SIG)).thenReturn(true);
        when(payloadParser.parse(BODY, "d-1")).thenReturn(event("closed"));

        ResponseEntity<Map<String, String>> response =
                controller.receive(BODY, SIG, "pull_request", "d-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ignored");
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void publishes_and_accepts_a_valid_opened_pull_request() throws Exception {
        PullRequestEvent opened = event("opened");
        when(signatureValidator.isValid(BODY, SIG)).thenReturn(true);
        when(payloadParser.parse(BODY, "d-1")).thenReturn(opened);

        ResponseEntity<Map<String, String>> response =
                controller.receive(BODY, SIG, "pull_request", "d-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "accepted");
        verify(eventPublisher).publish(opened);
    }

    @Test
    void returns_400_when_payload_is_malformed() throws Exception {
        when(signatureValidator.isValid(BODY, SIG)).thenReturn(true);
        when(payloadParser.parse(BODY, "d-1")).thenThrow(new java.io.IOException("bad json"));

        ResponseEntity<Map<String, String>> response =
                controller.receive(BODY, SIG, "pull_request", "d-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(eventPublisher, never()).publish(any());
    }
}
