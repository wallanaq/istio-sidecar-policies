package com.example.tokenization.api.controller;

import com.example.tokenization.api.dto.TokenizeRequest;
import com.example.tokenization.api.dto.TokenizeResponse;
import com.example.tokenization.domain.CardData;
import com.example.tokenization.domain.TokenizedCard;
import com.example.tokenization.service.TokenizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/card")
public class TokenizationController {

    private final TokenizationService tokenizationService;

    public TokenizationController(TokenizationService tokenizationService) {
        this.tokenizationService = tokenizationService;
    }

    @PostMapping("/tokenize")
    public ResponseEntity<TokenizeResponse> tokenize(@Valid @RequestBody TokenizeRequest request) {
        CardData cardData = new CardData(
                request.cardNumber(),
                request.holderName(),
                request.expirationMonth(),
                request.expirationYear(),
                request.cvv()
        );
        TokenizedCard result = tokenizationService.tokenize(cardData);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new TokenizeResponse(result.token(), result.brand(), result.createdAt()));
    }
}
