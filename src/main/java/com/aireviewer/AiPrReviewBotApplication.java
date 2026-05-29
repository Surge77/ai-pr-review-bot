package com.aireviewer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for the AI PR Review Bot.
 *
 * <p>Boots a Spring Boot context that wires the webhook receiver, Kafka pipeline,
 * Redis cache, Spring AI review service, GitHub client, audit store, and the
 * WebSocket progress channel. Infrastructure connectivity (PostgreSQL, Redis,
 * Kafka) is configured per profile in {@code application.yml}.
 */
@SpringBootApplication
public class AiPrReviewBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPrReviewBotApplication.class, args);
    }
}
