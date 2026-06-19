package com.example.tokenization.service.exception;

public class ExpiredCardException extends RuntimeException {

    public ExpiredCardException() {
        super("Card is expired");
    }
}
