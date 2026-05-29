package com.aireviewer.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the Spring AI {@link ChatClient} from the auto-configured builder. The
 * underlying provider (Groq locally, Gemini in prod) is selected purely by
 * profile configuration in {@code application.yml}.
 */
@Configuration
public class LlmConfig {

    /**
     * Creates the application's {@link ChatClient}.
     *
     * @param builder the Spring AI auto-configured chat client builder
     * @return a ready-to-use chat client
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
