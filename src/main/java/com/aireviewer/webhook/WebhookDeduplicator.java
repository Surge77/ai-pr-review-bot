package com.aireviewer.webhook;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Deduplicates GitHub webhook deliveries by their {@code X-GitHub-Delivery} id, so
 * a redelivered event is not reviewed twice. Backed by Redis {@code SETNX}: the
 * first request to claim a delivery id proceeds; later ones are dropped.
 *
 * <p><strong>Fail-open</strong> (consistent with the review cache): if Redis is
 * unavailable, the delivery is treated as first-seen and processed — availability
 * is preferred over strict once-only delivery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDeduplicator {

    private static final String KEY_PREFIX = "webhook:delivery:";
    private static final String MARKER_VALUE = "1";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    /**
     * Claims a delivery id for processing.
     *
     * @param deliveryId the {@code X-GitHub-Delivery} id (may be null/blank)
     * @return {@code true} if this is the first time the id is seen (or it is
     *         absent, or Redis is unavailable); {@code false} if it is a duplicate
     */
    public boolean isFirstDelivery(String deliveryId) {
        if (!StringUtils.hasText(deliveryId)) {
            return true;
        }
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(KEY_PREFIX + deliveryId, MARKER_VALUE, TTL);
            return !Boolean.FALSE.equals(acquired);
        } catch (Exception e) {
            log.warn("Redis unavailable during delivery dedup for {} ({}); processing anyway",
                    deliveryId, e.getMessage());
            return true;
        }
    }
}
