package com.example.purchasefx.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Treasury exchange-rate value object.
 *
 * <p>The Treasury dataset identifies currencies by {@code country_currency_desc},
 * for example {@code Canada-Dollar}. The rate is represented as {@link BigDecimal}
 * to preserve decimal precision for money calculations.</p>
 */
public record ExchangeRate(
        String countryCurrencyDescription,
        BigDecimal rate,
        LocalDate recordDate
) {}
