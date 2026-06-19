package com.example.binchecker.api.controller.advice;

import com.example.binchecker.api.dto.ErrorResponse;
import com.example.binchecker.service.exception.BinNotFoundException;
import com.example.binchecker.service.exception.InvalidBinException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BinNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBinNotFound(BinNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("BIN_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidBinException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBin(InvalidBinException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_BIN", ex.getMessage()));
    }
}
