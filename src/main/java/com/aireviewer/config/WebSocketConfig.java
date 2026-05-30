package com.aireviewer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket setup for the live progress dashboard: a single in-memory
 * broker exposes {@code /topic/**} for server-to-client broadcasts, and clients
 * connect at {@code /ws} (with SockJS fallback). Handshake origins are restricted
 * to the configured trusted hosts.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String STOMP_ENDPOINT = "/ws";
    private static final String BROKER_PREFIX = "/topic";
    private static final String APP_PREFIX = "/app";

    private final WebSocketProperties properties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(BROKER_PREFIX);
        registry.setApplicationDestinationPrefixes(APP_PREFIX);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = properties.allowedOrigins().toArray(String[]::new);
        registry.addEndpoint(STOMP_ENDPOINT)
                .setAllowedOrigins(origins)
                .withSockJS();
    }
}
