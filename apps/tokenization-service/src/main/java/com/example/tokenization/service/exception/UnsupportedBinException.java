package com.example.tokenization.service.exception;

public class UnsupportedBinException extends RuntimeException {

    public UnsupportedBinException(String bin) {
        super("BIN not supported: " + bin);
    }
}
