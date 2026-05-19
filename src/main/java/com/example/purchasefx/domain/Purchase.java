package com.example.purchasefx.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Core domain entity representing a stored purchase transaction.
 *
 * <p>The service layer validates business rules before this record is created:
 * description length, ISO transaction date, and positive USD amount rounded to cents.</p>
 */
public record Purchase(
        UUID id,
        String description,
        LocalDate transactionDate,
        BigDecimal purchaseAmountUsd
) {
    /** Performs defensive null checks so invalid domain objects are never stored. */
    public Purchase {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(description, "description is required");
        Objects.requireNonNull(transactionDate, "transactionDate is required");
        Objects.requireNonNull(purchaseAmountUsd, "purchaseAmountUsd is required");
    }
}
