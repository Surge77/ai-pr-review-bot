package com.aireviewer.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Application-wide exception handling. Extends {@link ResponseEntityExceptionHandler}
 * so Spring's framework exceptions keep their correct 4xx status, and adds a
 * catch-all that maps any unexpected error to a generic {@code 500} — the
 * underlying message is logged server-side but never returned to the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Maps any unhandled exception to a safe {@code 500} response.
     *
     * @param ex the unhandled exception
     * @return an RFC 7807 problem detail with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
