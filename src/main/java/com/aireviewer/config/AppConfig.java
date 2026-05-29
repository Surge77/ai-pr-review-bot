package com.aireviewer.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core application beans shared across the codebase.
 */
@Configuration
public class AppConfig {

    /**
     * Provides a UTC {@link Clock} so timestamp generation is injectable and
     * therefore deterministically testable.
     *
     * @return a fixed-zone system clock in UTC
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
