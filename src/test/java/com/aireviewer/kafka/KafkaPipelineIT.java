package com.aireviewer.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.aireviewer.audit.ReviewAuditLogRepository;
import com.aireviewer.model.PullRequestEvent;
import com.aireviewer.model.ReviewStatus;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end pipeline test on real Kafka + PostgreSQL: a published event reaches
 * the processor, and a repeatedly-failing event is retried, dead-lettered, and
 * recorded as FAILED in the audit log. Requires Docker (runs in CI).
 */
@Tag("integration")
@Testcontainers
@SpringBootTest
class KafkaPipelineIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"));

    @Container
    @ServiceConnection
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"));

    @Container
    static RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.ai.openai.api-key", () -> "test-key");
    }

    @MockitoBean
    private ReviewProcessor reviewProcessor;

    @Autowired
    private PullRequestEventPublisher publisher;

    @Autowired
    private ReviewAuditLogRepository auditRepository;

    private static PullRequestEvent event(int pr, String delivery) {
        return new PullRequestEvent(pr, "octo/repo", "octo", "head" + pr, "base" + pr,
                "url", "opened", "octo", delivery, Instant.parse("2026-05-29T00:00:00Z"));
    }

    @Test
    void published_event_is_consumed_and_processed() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(reviewProcessor).process(any());

        publisher.publish(event(101, "delivery-ok"));

        assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void repeatedly_failing_event_is_dead_lettered_and_audited_as_failed() {
        doThrow(new RuntimeException("processing failed")).when(reviewProcessor).process(any());

        publisher.publish(event(202, "delivery-fail"));

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500)).untilAsserted(() ->
                assertThat(auditRepository.findAll())
                        .anyMatch(r -> r.getPrNumber() == 202
                                && r.getStatus() == ReviewStatus.FAILED));
    }
}
