package com.example.purchasefx.domain;

/** Indicates that client-supplied input failed validation before persistence/conversion. */
public final class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
