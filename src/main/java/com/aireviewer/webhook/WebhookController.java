package com.aireviewer.webhook;

import java.util.Map;

import com.aireviewer.config.WebhookProperties;
import com.aireviewer.kafka.PullRequestEventPublisher;
import com.aireviewer.model.PullRequestEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives GitHub webhook deliveries. Validates the HMAC signature over the raw
 * body, filters to accepted pull-request actions, and hands valid events to the
 * Kafka pipeline — returning {@code 200} immediately so GitHub never blocks.
 */
@Slf4j
@Tag(name = "Webhooks", description = "GitHub webhook ingress")
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private static final String PULL_REQUEST_EVENT = "pull_request";

    private final GitHubSignatureValidator signatureValidator;
    private final WebhookPayloadParser payloadParser;
    private final PullRequestEventPublisher eventPublisher;
    private final WebhookProperties webhookProperties;
    private final WebhookRateLimiter rateLimiter;

    /**
     * Handles a GitHub webhook delivery.
     *
     * @param payload    raw request body (read before deserialization for HMAC)
     * @param signature  {@code X-Hub-Signature-256} header
     * @param eventType  {@code X-GitHub-Event} header
     * @param deliveryId {@code X-GitHub-Delivery} header (idempotency key)
     * @return {@code 429} when the rate limit is exceeded; {@code 401} if the
     *         signature is invalid; {@code 200} with {@code {"status":"ignored"}}
     *         for non-PR or unaccepted actions; {@code 200} with
     *         {@code {"status":"accepted"}} when queued
     */
    @Operation(summary = "Receive a GitHub webhook delivery")
    @PostMapping("/github")
    public ResponseEntity<Map<String, String>> receive(
            @RequestBody byte[] payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId) {

        if (!rateLimiter.tryAcquire()) {
            log.warn("Rate limit exceeded; rejecting webhook delivery={}", deliveryId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("status", "rate_limited"));
        }

        if (!signatureValidator.isValid(payload, signature)) {
            log.warn("Rejected webhook delivery={} : invalid or missing signature", deliveryId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "invalid_signature"));
        }

        if (!PULL_REQUEST_EVENT.equals(eventType)) {
            return ResponseEntity.ok(Map.of("status", "ignored"));
        }

        PullRequestEvent event;
        try {
            event = payloadParser.parse(payload, deliveryId);
        } catch (Exception e) {
            log.warn("Rejected webhook delivery={} : malformed payload", deliveryId);
            return ResponseEntity.badRequest().body(Map.of("status", "malformed_payload"));
        }

        if (!webhookProperties.acceptedActions().contains(event.action())) {
            return ResponseEntity.ok(Map.of("status", "ignored"));
        }

        eventPublisher.publish(event);
        log.info("Accepted PR event repo={} pr={} action={} delivery={}",
                event.repoFullName(), event.prNumber(), event.action(), deliveryId);
        return ResponseEntity.ok(Map.of("status", "accepted"));
    }
}
