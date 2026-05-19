package com.example.purchasefx.service;

import com.example.purchasefx.domain.ExchangeRate;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Port for retrieving Treasury exchange rates.
 *
 * <p>The domain service depends on this interface rather than an HTTP implementation,
 * which keeps conversion rules unit-testable and isolates external API concerns in
 * infrastructure code.</p>
 */
public interface ExchangeRateClient {
    /**
     * Finds the latest available exchange rate for the requested currency where
     * {@code earliestAllowedDate <= record_date <= purchaseDate}.
     */
    Optional<ExchangeRate> findRate(String countryCurrencyDescription, LocalDate purchaseDate, LocalDate earliestAllowedDate);
}
