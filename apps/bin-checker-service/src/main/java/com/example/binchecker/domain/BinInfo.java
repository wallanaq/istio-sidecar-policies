package com.example.binchecker.domain;

public record BinInfo(
        String bin,
        CardBrand brand,
        CardType type,
        String bank
) {}
