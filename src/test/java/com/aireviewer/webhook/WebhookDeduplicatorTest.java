package com.aireviewer.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class WebhookDeduplicatorTest {

    private static final String DELIVERY = "d-1";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private WebhookDeduplicator deduplicator;

    @BeforeEach
    void setUp() {
        deduplicator = new WebhookDeduplicator(redisTemplate);
    }

    private void stubSetIfAbsent(Boolean result) {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.setIfAbsent(eq("webhook:delivery:" + DELIVERY), any(), any(Duration.class)))
                .thenReturn(result);
    }

    @Test
    @DisplayName("first delivery claims the key and proceeds")
    void first_delivery_proceeds() {
        stubSetIfAbsent(true);
        assertThat(deduplicator.isFirstDelivery(DELIVERY)).isTrue();
    }

    @Test
    @DisplayName("duplicate delivery is rejected")
    void duplicate_rejected() {
        stubSetIfAbsent(false);
        assertThat(deduplicator.isFirstDelivery(DELIVERY)).isFalse();
    }

    @Test
    @DisplayName("blank delivery id is treated as first-seen without touching Redis")
    void blank_delivery_is_first() {
        assertThat(deduplicator.isFirstDelivery("  ")).isTrue();
        assertThat(deduplicator.isFirstDelivery(null)).isTrue();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("fails open: Redis error is treated as first-seen")
    void redis_error_fails_open() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));
        assertThat(deduplicator.isFirstDelivery(DELIVERY)).isTrue();
    }
}
