package com.example.tokenization.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TokenizeRequest(

        @NotBlank(message = "card number is required")
        @Pattern(regexp = "\\d{13,19}", message = "card number must contain between 13 and 19 digits")
        String cardNumber,

        @NotBlank(message = "holder name is required")
        String holderName,

        @NotNull(message = "expiration month is required")
        @Min(value = 1, message = "expiration month must be between 1 and 12")
        @Max(value = 12, message = "expiration month must be between 1 and 12")
        Integer expirationMonth,

        @NotNull(message = "expiration year is required")
        Integer expirationYear,

        @NotBlank(message = "cvv is required")
        @Pattern(regexp = "\\d{3,4}", message = "cvv must contain 3 or 4 digits")
        String cvv
) {}
