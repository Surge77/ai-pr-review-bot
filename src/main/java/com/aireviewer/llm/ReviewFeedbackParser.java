package com.aireviewer.llm;

import com.aireviewer.model.ReviewFeedback;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Parses the LLM's textual response into {@link ReviewFeedback}.
 *
 * <p>LLMs frequently wrap JSON in prose or markdown code fences, so the parser
 * extracts the outermost JSON object before parsing. Any failure yields
 * {@link ReviewFeedback#fallback()} rather than throwing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewFeedbackParser {

    private final ObjectMapper objectMapper;

    /**
     * Parses raw model output into structured feedback.
     *
     * @param raw the model's response text
     * @return parsed feedback, or a non-approving fallback if it cannot be parsed
     */
    public ReviewFeedback parse(String raw) {
        String json = extractJsonObject(raw);
        if (json == null) {
            log.warn("LLM response contained no JSON object; using fallback");
            return ReviewFeedback.fallback();
        }
        try {
            return objectMapper.readValue(json, ReviewFeedback.class);
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON ({}); using fallback", e.getMessage());
            return ReviewFeedback.fallback();
        }
    }

    private String extractJsonObject(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return raw.substring(start, end + 1);
    }
}
