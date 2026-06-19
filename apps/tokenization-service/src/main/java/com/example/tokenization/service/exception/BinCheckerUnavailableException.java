package com.example.tokenization.service.exception;

public class BinCheckerUnavailableException extends RuntimeException {

    public BinCheckerUnavailableException() {
        super("bin-checker-service is unavailable");
    }
}
