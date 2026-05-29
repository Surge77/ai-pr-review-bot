package com.aireviewer.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class HealthCheckControllerTest {

    private static final Instant FIXED = Instant.parse("2026-05-29T00:00:00Z");

    @Mock
    private HealthProbeService probes;

    private HealthCheckController controller;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED, ZoneOffset.UTC);
        controller = new HealthCheckController(probes, fixedClock);
    }

    @Test
    void returns_200_and_up_with_full_component_breakdown_when_all_dependencies_are_up() {
        when(probes.pingDatabase()).thenReturn(true);
        when(probes.pingRedis()).thenReturn(true);
        when(probes.pingKafka()).thenReturn(true);

        ResponseEntity<HealthReport> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        HealthReport body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo("UP");
        assertThat(body.timestamp()).isEqualTo(FIXED);
        assertThat(body.components())
                .containsEntry("db", "UP")
                .containsEntry("redis", "UP")
                .containsEntry("kafka", "UP")
                .hasSize(3);
    }

    @Test
    void returns_503_and_down_when_any_dependency_is_down() {
        when(probes.pingDatabase()).thenReturn(true);
        when(probes.pingRedis()).thenReturn(false);
        when(probes.pingKafka()).thenReturn(true);

        ResponseEntity<HealthReport> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        HealthReport body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo("DOWN");
        assertThat(body.components()).containsEntry("redis", "DOWN");
    }
}
