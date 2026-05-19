package com.example.purchasefx.service;

import com.example.purchasefx.domain.ConvertedPurchase;
import com.example.purchasefx.domain.CurrencyConversionException;
import com.example.purchasefx.domain.NotFoundException;
import com.example.purchasefx.domain.Purchase;
import com.example.purchasefx.domain.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Application/domain service containing the purchase and conversion business rules.
 *
 * <p>This class is intentionally independent of HTTP and file-system details. It can be
 * exercised directly by unit tests and is the single place where the product brief's
 * validation and six-month currency lookup rules are enforced.</p>
 */
public final class PurchaseService {
    private final PurchaseRepository repository;
    private final ExchangeRateClient exchangeRateClient;

    public PurchaseService(PurchaseRepository repository, ExchangeRateClient exchangeRateClient) {
        this.repository = repository;
        this.exchangeRateClient = exchangeRateClient;
    }

    /**
     * Validates and stores a purchase transaction.
     *
     * <p>A UUID is assigned at creation time. Purchase amounts are rounded to the nearest
     * cent using HALF_UP rounding before persistence.</p>
     */
    public Purchase store(String description, String transactionDateText, String purchaseAmountText) {
        var descriptionClean = validateDescription(description);
        var transactionDate = parseDate(transactionDateText);
        var purchaseAmountUsd = parsePositiveMoney(purchaseAmountText);
        var purchase = new Purchase(UUID.randomUUID(), descriptionClean, transactionDate, purchaseAmountUsd);
        try {
            return repository.save(purchase);
        } catch (IOException e) {
            throw new IllegalStateException("Purchase could not be persisted", e);
        }
    }

    /** Retrieves a stored purchase or throws a domain-level not-found exception. */
    public Purchase get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Purchase was not found"));
    }

    /**
     * Converts a stored USD purchase to the requested Treasury currency.
     *
     * <p>The exchange rate must be active on or before the purchase date and no older
     * than six months before the purchase date. The converted amount is rounded to two
     * decimals to satisfy the cent-level requirement.</p>
     */
    public ConvertedPurchase convert(UUID id, String countryCurrencyDescription) {
        if (countryCurrencyDescription == null || countryCurrencyDescription.isBlank()) {
            throw new ValidationException("Target currency is required. Use the Treasury country-currency description, for example Canada-Dollar.");
        }
        var purchase = get(id);
        var earliestAllowed = purchase.transactionDate().minusMonths(6);
        var rate = exchangeRateClient.findRate(countryCurrencyDescription.trim(), purchase.transactionDate(), earliestAllowed)
                .orElseThrow(() -> new CurrencyConversionException(
                        "Purchase cannot be converted to the target currency because no Treasury exchange rate was available within 6 months on or before the purchase date."));

        var convertedAmount = purchase.purchaseAmountUsd().multiply(rate.rate()).setScale(2, RoundingMode.HALF_UP);
        return new ConvertedPurchase(
                purchase.id(),
                purchase.description(),
                purchase.transactionDate(),
                purchase.purchaseAmountUsd(),
                rate.countryCurrencyDescription(),
                rate.rate(),
                rate.recordDate(),
                convertedAmount
        );
    }

    /** Validates the required 50-character description limit. */
    private static String validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new ValidationException("Description is required");
        }
        var clean = description.trim();
        if (clean.length() > 50) {
            throw new ValidationException("Description must not exceed 50 characters");
        }
        return clean;
    }

    /** Parses an ISO-8601 yyyy-MM-dd transaction date. */
    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Transaction date is required and must use ISO-8601 format yyyy-MM-dd");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Transaction date must be a valid ISO-8601 date in yyyy-MM-dd format");
        }
    }

    /** Parses, validates, and rounds a positive USD amount to two decimal places. */
    private static BigDecimal parsePositiveMoney(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Purchase amount is required");
        }
        try {
            var amount = new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Purchase amount must be positive");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new ValidationException("Purchase amount must be a valid decimal amount");
        }
    }
}
