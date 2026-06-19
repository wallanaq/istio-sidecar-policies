package com.example.binchecker.service;

import com.example.binchecker.domain.BinInfo;
import com.example.binchecker.repository.BinRepository;
import com.example.binchecker.service.exception.BinNotFoundException;
import com.example.binchecker.service.exception.InvalidBinException;
import org.springframework.stereotype.Service;

@Service
public class BinService {

    private static final String BIN_PATTERN = "\\d{6}";

    private final BinRepository binRepository;

    public BinService(BinRepository binRepository) {
        this.binRepository = binRepository;
    }

    public BinInfo find(String bin) {
        if (!bin.matches(BIN_PATTERN)) {
            throw new InvalidBinException();
        }
        return binRepository.findByBin(bin)
                .orElseThrow(() -> new BinNotFoundException(bin));
    }
}
