package com.example.binchecker.service.exception;

public class InvalidBinException extends RuntimeException {

    public InvalidBinException() {
        super("BIN must be 6 numeric digits");
    }
}
