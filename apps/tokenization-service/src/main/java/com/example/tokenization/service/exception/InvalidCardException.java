package com.example.tokenization.service.exception;

public class InvalidCardException extends RuntimeException {

    public InvalidCardException(String message) {
        super(message);
    }
}
