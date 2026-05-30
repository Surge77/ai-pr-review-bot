package com.aireviewer.llm;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.aireviewer.config.LlmProperties;
import com.aireviewer.model.ReviewFeedback;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Runs an LLM code review for a single file/chunk via Spring AI.
 *
 * <p>Each call is bounded by a configurable timeout and runs on a virtual thread.
 * Failures (timeout, transport, provider error) are isolated: the method returns
 * an empty {@link Optional} so the caller can mark just that file FAILED and
 * continue reviewing the rest.
 */
@Slf4j
@Service
public class LLMReviewService {

    private final ChatClient chatClient;
    private final PromptTemplateLoader promptLoader;
    private final ReviewFeedbackParser feedbackParser;
    private final LlmProperties properties;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public LLMReviewService(ChatClient chatClient, PromptTemplateLoader promptLoader,
                            ReviewFeedbackParser feedbackParser, LlmProperties properties) {
        this.chatClient = chatClient;
        this.promptLoader = promptLoader;
        this.feedbackParser = feedbackParser;
        this.properties = properties;
    }

    /**
     * Reviews a single file's diff.
     *
     * @param filename the file under review
     * @param patch    the diff content
     * @return parsed feedback (possibly a parse-fallback), or empty if the LLM call
     *         failed or timed out
     */
    public Optional<ReviewFeedback> review(String filename, String patch) {
        String prompt = promptLoader.render(filename, patch);
        Future<String> future = executor.submit(
                () -> chatClient.prompt().user(prompt).call().content());
        try {
            String content = future.get(properties.timeoutSeconds(), TimeUnit.SECONDS);
            return Optional.of(feedbackParser.parse(content));
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("LLM review timed out after {}s for {}", properties.timeoutSeconds(), filename);
            return Optional.empty();
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            log.error("LLM review interrupted for {}", filename);
            return Optional.empty();
        } catch (Exception e) {
            future.cancel(true);
            log.error("LLM review failed for {}: {}", filename, e.getMessage());
            return Optional.empty();
        }
    }

    /** Shuts down the review executor when the bean is destroyed. */
    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
