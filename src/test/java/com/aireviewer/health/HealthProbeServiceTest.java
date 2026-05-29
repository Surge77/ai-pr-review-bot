package com.aireviewer.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.core.KafkaAdmin;

@ExtendWith(MockitoExtension.class)
class HealthProbeServiceTest {

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private RedisConnectionFactory redisConnectionFactory;
    @Mock private RedisConnection redisConnection;
    @Mock private KafkaAdmin kafkaAdmin;

    @org.mockito.InjectMocks
    private HealthProbeService service;

    @Test
    void pingDatabase_returns_true_when_connection_is_valid() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        assertThat(service.pingDatabase()).isTrue();
    }

    @Test
    void pingDatabase_returns_false_when_connection_cannot_be_obtained() throws Exception {
        when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("no route to host"));

        assertThat(service.pingDatabase()).isFalse();
    }

    @Test
    void pingRedis_returns_true_when_ping_returns_pong() {
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        assertThat(service.pingRedis()).isTrue();
    }

    @Test
    void pingRedis_returns_false_when_connection_factory_throws() {
        when(redisConnectionFactory.getConnection())
                .thenThrow(new org.springframework.dao.QueryTimeoutException("redis down"));

        assertThat(service.pingRedis()).isFalse();
    }

    @Test
    void pingKafka_returns_false_when_admin_client_cannot_be_built() {
        // Empty config (no bootstrap.servers) makes AdminClient.create fail fast,
        // exercising the probe's fail-safe catch branch without a live broker.
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(new java.util.HashMap<>());

        assertThat(service.pingKafka()).isFalse();
    }

    @Test
    void pingKafka_returns_true_against_a_running_broker() {
        // Embedded KRaft broker runs in-JVM (no Docker), covering the success path.
        var broker = new org.springframework.kafka.test.EmbeddedKafkaKraftBroker(1, 1);
        broker.afterPropertiesSet();
        try {
            when(kafkaAdmin.getConfigurationProperties())
                    .thenReturn(java.util.Map.of("bootstrap.servers", broker.getBrokersAsString()));

            assertThat(service.pingKafka()).isTrue();
        } finally {
            broker.destroy();
        }
    }
}
