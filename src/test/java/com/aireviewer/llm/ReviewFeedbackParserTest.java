package com.aireviewer.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.aireviewer.model.ReviewFeedback;
import com.aireviewer.model.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ReviewFeedbackParserTest {

    private final ReviewFeedbackParser parser = new ReviewFeedbackParser(new ObjectMapper());

    @Test
    void parses_valid_feedback_json() {
        String json = """
                {
                  "summary": "Adds null check",
                  "issues": [
                    {"line": 12, "severity": "CRITICAL", "comment": "NPE risk"},
                    {"line": null, "severity": "SUGGESTION", "comment": "Rename var"}
                  ],
                  "approved": false
                }
                """;

        ReviewFeedback fb = parser.parse(json);

        assertThat(fb.summary()).isEqualTo("Adds null check");
        assertThat(fb.approved()).isFalse();
        assertThat(fb.issueCount()).isEqualTo(2);
        assertThat(fb.hasCritical()).isTrue();
        assertThat(fb.issues().get(0).line()).isEqualTo(12);
        assertThat(fb.issues().get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(fb.issues().get(1).line()).isNull();
    }

    @Test
    void extracts_json_wrapped_in_markdown_fences_and_prose() {
        String raw = "Here is the review:\n```json\n{\"summary\":\"ok\",\"issues\":[],\"approved\":true}\n```\nThanks!";

        ReviewFeedback fb = parser.parse(raw);

        assertThat(fb.summary()).isEqualTo("ok");
        assertThat(fb.approved()).isTrue();
        assertThat(fb.issueCount()).isZero();
    }

    @Test
    void unknown_severity_falls_back_to_suggestion() {
        String json = "{\"summary\":\"s\",\"issues\":[{\"line\":1,\"severity\":\"BLOCKER\",\"comment\":\"c\"}],\"approved\":true}";

        ReviewFeedback fb = parser.parse(json);

        assertThat(fb.issues().getFirst().severity()).isEqualTo(Severity.SUGGESTION);
    }

    @Test
    void malformed_json_returns_non_approving_fallback_without_throwing() {
        ReviewFeedback fb = parser.parse("this is not json at all");

        assertThat(fb.approved()).isFalse();
        assertThat(fb.issueCount()).isZero();
        assertThat(fb.summary()).contains("could not parse");
    }

    @Test
    void empty_response_returns_fallback() {
        assertThat(parser.parse("").approved()).isFalse();
        assertThat(parser.parse(null).approved()).isFalse();
    }
}
