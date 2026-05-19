package com.example.purchasefx.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read model returned by the conversion use case.
 *
 * <p>It includes both the original purchase fields and the exchange-rate details used
 * to perform the conversion, so API consumers can audit how the converted amount was
 * calculated.</p>
 */
public record ConvertedPurchase(
        UUID id,
        String description,
        LocalDate transactionDate,
        BigDecimal purchaseAmountUsd,
        String targetCurrency,
        BigDecimal exchangeRateUsed,
        LocalDate exchangeRateDate,
        BigDecimal convertedAmount
) {}
