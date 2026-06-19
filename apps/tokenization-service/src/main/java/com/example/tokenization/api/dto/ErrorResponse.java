package com.example.tokenization.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<FieldViolation> violations
) {
    public ErrorResponse(String code, String message, Instant timestamp) {
        this(code, message, timestamp, null);
    }
}
