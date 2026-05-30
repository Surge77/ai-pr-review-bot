package com.aireviewer.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void maps_unexpected_exception_to_generic_500_without_leaking_detail() {
        ProblemDetail problem = handler.handleUnexpected(
                new RuntimeException("sensitive internal detail: jdbc://secret"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("Internal server error");
        assertThat(problem.getDetail()).doesNotContain("jdbc").doesNotContain("sensitive");
    }
}
