package com.example.purchasefx.infra;

import com.example.purchasefx.domain.CurrencyConversionException;
import com.example.purchasefx.domain.ExchangeRate;
import com.example.purchasefx.service.ExchangeRateClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * HTTP implementation of {@link ExchangeRateClient} backed by the Treasury FiscalData API.
 *
 * <p>The client asks Treasury for the most recent rate whose {@code record_date} is on
 * or before the purchase date and within the allowed six-month window. It requests only
 * the fields required by the product brief and uses {@code page[size]=1} with descending
 * date sort to avoid downloading unnecessary records.</p>
 */
public final class TreasuryFiscalDataClient implements ExchangeRateClient {
    private final String baseUrl;
    private final HttpClient httpClient;

    /**
     * Creates a Treasury client that calls the configured live FiscalData endpoint.
     *
     * @param baseUrl Treasury FiscalData API endpoint from application properties
     */
    public TreasuryFiscalDataClient(String baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /**
     * Calls Treasury FiscalData and returns the latest applicable rate, if one exists.
     *
     * <p>Any network, interruption, or non-success HTTP status is translated into a
     * domain-specific conversion exception so callers do not need to know HTTP details.</p>
     */
    @Override
    public Optional<ExchangeRate> findRate(String countryCurrencyDescription, LocalDate purchaseDate, LocalDate earliestAllowedDate) {
        var filter = "country_currency_desc:eq:" + countryCurrencyDescription
                + ",record_date:lte:" + purchaseDate
                + ",record_date:gte:" + earliestAllowedDate;
        var uri = URI.create(baseUrl + "?fields=country_currency_desc,exchange_rate,record_date"
                + "&filter=" + encode(filter)
                + "&sort=-record_date&page[size]=1");
        var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new CurrencyConversionException("Treasury exchange-rate API returned HTTP " + response.statusCode());
            }
            return parseFirstRate(response.body());
        } catch (IOException e) {
            throw new CurrencyConversionException("Unable to reach Treasury exchange-rate API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CurrencyConversionException("Treasury exchange-rate API request was interrupted", e);
        }
    }

    /** URL-encodes the FiscalData filter expression for safe query-string usage. */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Parses the first exchange-rate object from the Treasury response body.
     *
     * <p>This method is public to support direct unit testing of Treasury response parsing
     * without making a live network call.</p>
     */
    public static Optional<ExchangeRate> parseFirstRate(String body) {
        int dataStart = body.indexOf("\"data\"");
        if (dataStart < 0) return Optional.empty();
        int firstObject = body.indexOf('{', dataStart);
        int dataEnd = body.indexOf(']', dataStart);
        if (firstObject < 0 || dataEnd < firstObject) return Optional.empty();
        var object = body.substring(firstObject, body.indexOf('}', firstObject) + 1);
        var desc = Json.stringValue(object, "country_currency_desc");
        var rate = new BigDecimal(Json.stringValue(object, "exchange_rate").replace(",", ""));
        var recordDate = LocalDate.parse(Json.stringValue(object, "record_date"));
        return Optional.of(new ExchangeRate(desc, rate, recordDate));
    }
}
