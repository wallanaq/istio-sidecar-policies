package com.example.tokenization.client;

public class BinNotFoundException extends RuntimeException {

    public BinNotFoundException(String bin) {
        super("BIN not found: " + bin);
    }
}
