package com.aireviewer.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import com.aireviewer.model.FileDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CacheCheckServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private final CacheKeyStrategy keyStrategy = new CacheKeyStrategy();

    private CacheCheckService service() {
        return new CacheCheckService(redisTemplate, keyStrategy);
    }

    private static FileDiff file(String status) {
        return new FileDiff("src/Main.java", status, "@@ patch @@", "sha", 1, 0);
    }

    @Test
    void added_files_always_bypass_the_cache_without_touching_redis() {
        boolean reviewed = service().isAlreadyReviewed("octo/repo", file("added"));

        assertThat(reviewed).isFalse();
        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    void returns_true_on_cache_hit() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        assertThat(service().isAlreadyReviewed("octo/repo", file("modified"))).isTrue();
    }

    @Test
    void returns_false_on_cache_miss() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        assertThat(service().isAlreadyReviewed("octo/repo", file("modified"))).isFalse();
    }

    @Test
    void fails_open_when_redis_is_down_during_check() {
        when(redisTemplate.hasKey(anyString()))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertThat(service().isAlreadyReviewed("octo/repo", file("modified"))).isFalse();
    }

    @Test
    void mark_reviewed_stores_key_with_seven_day_ttl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        FileDiff file = file("modified");
        String expectedKey = keyStrategy.key("octo/repo", file.filename(), file.patch());

        service().markReviewed("octo/repo", file);

        verify(valueOps).set(eq(expectedKey), eq("1"), eq(Duration.ofDays(7)));
    }

    @Test
    void mark_reviewed_fails_open_when_redis_is_down() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        org.mockito.Mockito.doThrow(new RedisConnectionFailureException("down"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));

        // Should not throw.
        service().markReviewed("octo/repo", file("modified"));
    }
}
