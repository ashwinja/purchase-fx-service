package com.example.purchasefx.domain;

/** Indicates that a requested purchase identifier does not exist in storage. */
public final class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
