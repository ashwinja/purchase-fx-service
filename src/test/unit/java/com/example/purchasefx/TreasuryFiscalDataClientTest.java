package com.example.purchasefx;

import com.example.purchasefx.infra.TreasuryFiscalDataClient;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Unit test for parsing Treasury FiscalData API responses.
 *
 * <p>The application always calls the live Treasury API during runtime. This unit
 * test exercises only the JSON parser with a representative Treasury response payload
 * so the parser behavior remains deterministic.</p>
 */
public final class TreasuryFiscalDataClientTest {
    /** Runs the parser test from the command-line test script. */
    public static void main(String[] args) {
        var json = "{\"data\":[{\"country_currency_desc\":\"Canada-Dollar\",\"exchange_rate\":\"1.3690\",\"record_date\":\"2025-12-31\"}],\"meta\":{}}";
        var rate = TreasuryFiscalDataClient.parseFirstRate(json).orElseThrow();
        if (!rate.countryCurrencyDescription().equals("Canada-Dollar")) throw new AssertionError("currency desc");
        if (!rate.rate().equals(new BigDecimal("1.3690"))) throw new AssertionError("rate");
        if (!rate.recordDate().equals(LocalDate.parse("2025-12-31"))) throw new AssertionError("date");
        System.out.println("TreasuryFiscalDataClientTest passed");
    }
}
