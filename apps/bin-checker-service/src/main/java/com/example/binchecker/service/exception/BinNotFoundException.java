package com.example.binchecker.service.exception;

public class BinNotFoundException extends RuntimeException {

    public BinNotFoundException(String bin) {
        super("BIN not found: " + bin);
    }
}
