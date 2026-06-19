package com.example.tokenization.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("clients.bin-checker")
public record BinCheckerProperties(
        String url,
        int connectTimeout,
        int readTimeout
) {}
