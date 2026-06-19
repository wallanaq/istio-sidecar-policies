package com.example.tokenization.service;

import com.example.tokenization.client.BinCheckerClient;
import com.example.tokenization.client.BinNotFoundException;
import com.example.tokenization.domain.CardData;
import com.example.tokenization.domain.TokenizedCard;
import com.example.tokenization.service.exception.ExpiredCardException;
import com.example.tokenization.service.exception.InvalidCardException;
import com.example.tokenization.service.exception.UnsupportedBinException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class TokenizationService {

    private final BinCheckerClient binCheckerClient;

    public TokenizationService(BinCheckerClient binCheckerClient) {
        this.binCheckerClient = binCheckerClient;
    }

    public TokenizedCard tokenize(CardData card) {
        validateLuhn(card.cardNumber());
        validateExpiry(card.expirationYear(), card.expirationMonth());

        String bin = card.cardNumber().substring(0, 6);
        var binInfo = findBin(bin);

        return new TokenizedCard(UUID.randomUUID().toString(), binInfo.brand(), Instant.now());
    }

    private void validateLuhn(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            alternate = !alternate;
        }
        if (sum % 10 != 0) {
            throw new InvalidCardException("Invalid card number");
        }
    }

    private void validateExpiry(int year, int month) {
        if (YearMonth.of(year, month).isBefore(YearMonth.now())) {
            throw new ExpiredCardException();
        }
    }

    private com.example.tokenization.client.BinInfo findBin(String bin) {
        try {
            return binCheckerClient.findByBin(bin);
        } catch (BinNotFoundException e) {
            throw new UnsupportedBinException(bin);
        }
    }
}
