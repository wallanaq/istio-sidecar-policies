package com.example.tokenization.api.controller.advice;

import com.example.tokenization.api.dto.ErrorResponse;
import com.example.tokenization.api.dto.FieldViolation;
import com.example.tokenization.service.exception.BinCheckerUnavailableException;
import com.example.tokenization.service.exception.ExpiredCardException;
import com.example.tokenization.service.exception.InvalidCardException;
import com.example.tokenization.service.exception.UnsupportedBinException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnsupportedBinException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedBin(UnsupportedBinException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("UNSUPPORTED_BIN", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(ExpiredCardException.class)
    public ResponseEntity<ErrorResponse> handleExpiredCard(ExpiredCardException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("EXPIRED_CARD", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(InvalidCardException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCard(InvalidCardException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_CARD_DATA", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(BinCheckerUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleBinCheckerUnavailable(BinCheckerUnavailableException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("BIN_CHECKER_UNAVAILABLE", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldViolation(toSnakeCase(e.getField()), e.getDefaultMessage()))
                .toList();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_CARD_DATA", "Validation failed", Instant.now(), violations));
    }

    private String toSnakeCase(String camel) {
        return camel.replaceAll("([A-Z])", "_$1").toLowerCase();
    }
}
