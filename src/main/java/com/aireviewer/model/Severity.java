package com.aireviewer.model;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Severity of a review issue. Tolerant of unexpected LLM output: any unrecognized
 * value maps to {@link #SUGGESTION} rather than failing parsing.
 */
public enum Severity {
    CRITICAL,
    WARNING,
    SUGGESTION;

    /**
     * Maps a raw severity string to an enum value, defaulting to
     * {@link #SUGGESTION} for null/blank/unknown input.
     *
     * @param value raw severity from the model
     * @return the matching severity, or {@link #SUGGESTION}
     */
    @JsonCreator
    public static Severity fromValue(String value) {
        if (value == null || value.isBlank()) {
            return SUGGESTION;
        }
        try {
            return Severity.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SUGGESTION;
        }
    }
}
