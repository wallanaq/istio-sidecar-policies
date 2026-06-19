package com.example.tokenization.domain;

import java.time.Instant;

public record TokenizedCard(
        String token,
        String brand,
        Instant createdAt
) {}
