package com.aireviewer;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Phase 1 integration test: the full Spring context loads against real
 * PostgreSQL, Kafka, and Redis containers, and Flyway has applied V1 (verified
 * by the presence of the review_audit_log table). Requires a running Docker.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest
class ApplicationContextIT {

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
        // Spring AI openai autoconfig needs a key present; no call is made at startup.
        registry.add("spring.ai.openai.api-key", () -> "test-key");
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void context_loads_and_flyway_created_the_audit_table() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Integer tableCount = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'review_audit_log'",
                Integer.class);

        assertThat(tableCount).isEqualTo(1);
    }
}
