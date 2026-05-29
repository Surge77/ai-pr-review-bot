package com.aireviewer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger UI metadata. The interactive docs are served at
 * {@code /swagger-ui.html} and the raw spec at {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the top-level OpenAPI document describing this service.
     *
     * @return the configured {@link OpenAPI} metadata bean
     */
    @Bean
    public OpenAPI aiPrReviewBotOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI PR Review Bot API")
                        .description("AI-powered GitHub Pull Request code review system")
                        .version("0.1.0")
                        .contact(new Contact().name("ai-pr-review-bot"))
                        .license(new License().name("MIT")));
    }
}
