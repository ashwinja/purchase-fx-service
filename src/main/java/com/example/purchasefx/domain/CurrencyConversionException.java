package com.example.purchasefx.domain;

/**
 * Signals that a stored purchase could not be converted to the requested currency.
 *
 * <p>Typical causes are a missing Treasury rate within the allowed six-month window
 * or an upstream Treasury API failure.</p>
 */
public final class CurrencyConversionException extends RuntimeException {
    public CurrencyConversionException(String message) {
        super(message);
    }

    public CurrencyConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
