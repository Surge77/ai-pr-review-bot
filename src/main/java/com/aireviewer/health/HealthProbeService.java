package com.aireviewer.health;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

/**
 * Performs live connectivity probes against the backing infrastructure.
 *
 * <p>Each probe is isolated and fail-safe: any exception is swallowed and
 * reported as a {@code false} (DOWN) result rather than propagating, so that one
 * unavailable dependency never masks the health of the others.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthProbeService {

    private static final int DB_VALIDATION_TIMEOUT_SECONDS = 2;
    private static final long KAFKA_DESCRIBE_TIMEOUT_SECONDS = 3;

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final KafkaAdmin kafkaAdmin;

    /**
     * Verifies a usable JDBC connection to PostgreSQL can be obtained.
     *
     * @return {@code true} if the connection validates within the timeout
     */
    public boolean pingDatabase() {
        try (var connection = dataSource.getConnection()) {
            return connection.isValid(DB_VALIDATION_TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.warn("Database health probe failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Issues a Redis {@code PING} and checks for the expected pong reply.
     *
     * @return {@code true} if Redis responds to PING
     */
    public boolean pingRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping());
        } catch (Exception e) {
            log.warn("Redis health probe failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Describes the Kafka cluster to confirm broker reachability.
     *
     * @return {@code true} if a cluster id is returned within the timeout
     */
    public boolean pingKafka() {
        Properties config = new Properties();
        config.putAll(kafkaAdmin.getConfigurationProperties());
        try (AdminClient admin = AdminClient.create(config)) {
            admin.describeCluster()
                    .clusterId()
                    .get(KAFKA_DESCRIBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Kafka health probe interrupted");
            return false;
        } catch (Exception e) {
            log.warn("Kafka health probe failed: {}", e.getMessage());
            return false;
        }
    }
}
