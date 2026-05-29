package com.aireviewer.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.aireviewer.model.FileDiff;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.utility.DockerImageName;

/**
 * Cache behavior against a real Redis (Testcontainers): an identical diff is a hit
 * after being marked, while {@code added} files always bypass. Requires Docker.
 */
@Tag("integration")
class CacheCheckIT {

    private static RedisContainer redis;
    private static LettuceConnectionFactory connectionFactory;
    private static CacheCheckService service;

    @BeforeAll
    static void startRedis() {
        redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));
        redis.start();

        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        service = new CacheCheckService(template, new CacheKeyStrategy());
    }

    @AfterAll
    static void stopRedis() {
        connectionFactory.destroy();
        redis.stop();
    }

    private static FileDiff file(String name, String status, String patch) {
        return new FileDiff(name, status, patch, "sha", 1, 0);
    }

    @Test
    void identical_diff_is_a_hit_after_being_marked_reviewed() {
        FileDiff f = file("src/A.java", "modified", "@@ patch A @@");

        assertThat(service.isAlreadyReviewed("octo/repo", f)).isFalse();
        service.markReviewed("octo/repo", f);
        assertThat(service.isAlreadyReviewed("octo/repo", f)).isTrue();
    }

    @Test
    void added_file_bypasses_cache_even_after_marking() {
        FileDiff f = file("src/New.java", "added", "@@ new file @@");

        service.markReviewed("octo/repo", f);

        assertThat(service.isAlreadyReviewed("octo/repo", f)).isFalse();
    }
}
