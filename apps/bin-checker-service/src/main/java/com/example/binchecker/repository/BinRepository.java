package com.example.binchecker.repository;

import com.example.binchecker.domain.BinInfo;
import com.example.binchecker.domain.CardBrand;
import com.example.binchecker.domain.CardType;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class BinRepository {

    private static final Map<String, BinInfo> BINS = new HashMap<>();

    static {
        // VISA CREDIT
        BINS.put("411111", new BinInfo("411111", CardBrand.VISA, CardType.CREDIT, "Banco do Brasil"));
        BINS.put("457393", new BinInfo("457393", CardBrand.VISA, CardType.CREDIT, "Itaú Unibanco"));

        // VISA DEBIT
        BINS.put("400699", new BinInfo("400699", CardBrand.VISA, CardType.DEBIT, "Bradesco"));
        BINS.put("438935", new BinInfo("438935", CardBrand.VISA, CardType.DEBIT, "Caixa Econômica Federal"));

        // MASTERCARD CREDIT
        BINS.put("510510", new BinInfo("510510", CardBrand.MASTERCARD, CardType.CREDIT, "Itaú Unibanco"));
        BINS.put("554801", new BinInfo("554801", CardBrand.MASTERCARD, CardType.CREDIT, "Santander"));

        // MASTERCARD DEBIT
        BINS.put("516220", new BinInfo("516220", CardBrand.MASTERCARD, CardType.DEBIT, "Bradesco"));
        BINS.put("526492", new BinInfo("526492", CardBrand.MASTERCARD, CardType.DEBIT, "Nubank"));

        // AMEX
        BINS.put("376449", new BinInfo("376449", CardBrand.AMEX, CardType.CREDIT, "American Express"));
        BINS.put("341234", new BinInfo("341234", CardBrand.AMEX, CardType.CREDIT, "American Express"));

        // ELO CREDIT
        BINS.put("636368", new BinInfo("636368", CardBrand.ELO, CardType.CREDIT, "Caixa Econômica Federal"));
        BINS.put("650485", new BinInfo("650485", CardBrand.ELO, CardType.CREDIT, "Banco do Brasil"));

        // ELO DEBIT
        BINS.put("650906", new BinInfo("650906", CardBrand.ELO, CardType.DEBIT, "Bradesco"));
        BINS.put("651652", new BinInfo("651652", CardBrand.ELO, CardType.DEBIT, "Santander"));

        // HIPERCARD
        BINS.put("606282", new BinInfo("606282", CardBrand.HIPERCARD, CardType.CREDIT, "Hipercard"));
        BINS.put("384100", new BinInfo("384100", CardBrand.HIPERCARD, CardType.CREDIT, "Itaú Unibanco"));

        // DINERS
        BINS.put("301234", new BinInfo("301234", CardBrand.DINERS, CardType.CREDIT, "Citibank"));
        BINS.put("305693", new BinInfo("305693", CardBrand.DINERS, CardType.CREDIT, "Banco do Brasil"));
    }

    public Optional<BinInfo> findByBin(String bin) {
        return Optional.ofNullable(BINS.get(bin));
    }
}
