package com.example.tokenization.domain;

public record CardData(
        String cardNumber,
        String holderName,
        Integer expirationMonth,
        Integer expirationYear,
        String cvv
) {}
