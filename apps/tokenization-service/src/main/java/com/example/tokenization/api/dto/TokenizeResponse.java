package com.example.tokenization.api.dto;

import java.time.Instant;

public record TokenizeResponse(
        String token,
        String brand,
        Instant createdAt
) {}
