package com.example.binchecker.api.dto;

import com.example.binchecker.domain.BinInfo;
import com.example.binchecker.domain.CardBrand;
import com.example.binchecker.domain.CardType;

public record BinResponse(
        String bin,
        CardBrand brand,
        CardType type,
        String bank
) {
    public static BinResponse from(BinInfo info) {
        return new BinResponse(info.bin(), info.brand(), info.type(), info.bank());
    }
}
