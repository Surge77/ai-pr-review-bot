package com.aireviewer.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code app.websocket.*}.
 *
 * @param allowedOrigins origins permitted to open the STOMP/SockJS handshake;
 *                       restricted to trusted hosts to prevent cross-site socket hijacking
 */
@ConfigurationProperties(prefix = "app.websocket")
public record WebSocketProperties(List<String> allowedOrigins) {

    public WebSocketProperties {
        allowedOrigins = allowedOrigins == null || allowedOrigins.isEmpty()
                ? List.of("http://localhost:8080")
                : List.copyOf(allowedOrigins);
    }
}
