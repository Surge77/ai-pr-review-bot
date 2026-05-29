package com.aireviewer.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.aireviewer.config.LlmProperties;
import com.aireviewer.model.ReviewFeedback;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

class LLMReviewServiceTest {

    private ChatClient chatClient;
    private LLMReviewService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        PromptTemplateLoader loader = new PromptTemplateLoader();
        ReflectionTestUtils.invokeMethod(loader, "load");
        service = new LLMReviewService(chatClient, loader,
                new ReviewFeedbackParser(new ObjectMapper()), new LlmProperties(5));
    }

    private void stubModelResponse(String content) {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn(content);
    }

    @Test
    void returns_parsed_feedback_for_a_valid_model_response() {
        stubModelResponse("{\"summary\":\"ok\",\"issues\":[],\"approved\":true}");

        Optional<ReviewFeedback> result = service.review("Main.java", "@@ patch @@");

        assertThat(result).isPresent();
        assertThat(result.get().summary()).isEqualTo("ok");
        assertThat(result.get().approved()).isTrue();
    }

    @Test
    void returns_empty_when_the_llm_call_fails() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenThrow(new RuntimeException("provider unavailable"));

        Optional<ReviewFeedback> result = service.review("Main.java", "@@ patch @@");

        assertThat(result).isEmpty();
    }
}
