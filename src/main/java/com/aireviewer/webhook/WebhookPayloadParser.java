package com.aireviewer.webhook;

import java.time.Clock;

import com.aireviewer.model.PullRequestEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Maps a GitHub {@code pull_request} webhook payload into the internal
 * {@link PullRequestEvent}. Uses lenient {@code path()} navigation so unexpected
 * or extra fields never throw; missing fields resolve to empty/zero values.
 */
@Component
@RequiredArgsConstructor
public class WebhookPayloadParser {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Parses the raw webhook body into a {@link PullRequestEvent}.
     *
     * @param payload    raw request body bytes
     * @param deliveryId GitHub {@code X-GitHub-Delivery} id (idempotency key)
     * @return the mapped event
     * @throws java.io.IOException if the body is not valid JSON
     */
    public PullRequestEvent parse(byte[] payload, String deliveryId) throws java.io.IOException {
        JsonNode root = objectMapper.readTree(payload);
        JsonNode pr = root.path("pull_request");
        JsonNode repo = root.path("repository");

        return new PullRequestEvent(
                pr.path("number").asInt(root.path("number").asInt()),
                repo.path("full_name").asText(),
                repo.path("owner").path("login").asText(),
                pr.path("head").path("sha").asText(),
                pr.path("base").path("sha").asText(),
                pr.path("diff_url").asText(),
                root.path("action").asText(),
                root.path("sender").path("login").asText(),
                deliveryId,
                clock.instant());
    }
}
