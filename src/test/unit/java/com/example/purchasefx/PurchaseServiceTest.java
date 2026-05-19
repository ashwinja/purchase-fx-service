package com.example.purchasefx;

import com.example.purchasefx.domain.ExchangeRate;
import com.example.purchasefx.domain.ValidationException;
import com.example.purchasefx.infra.FilePurchaseRepository;
import com.example.purchasefx.service.ExchangeRateClient;
import com.example.purchasefx.service.PurchaseService;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Unit tests for PurchaseService.
 *
 * <p>These tests use a temporary file repository and an in-memory ExchangeRateClient
 * implementation so the service rules are tested without network access. A production
 * codebase would usually use JUnit and a mocking framework such as Mockito; this
 * project uses tiny local helpers to avoid requiring additional dependency downloads.</p>
 */
public final class PurchaseServiceTest {
    /** Runs all service tests from the command-line test script. */
    public static void main(String[] args) throws Exception {
        storesRoundedAmountAndConverts();
        rejectsInvalidDescription();
        rejectsNoRateWithinSixMonths();
        System.out.println("PurchaseServiceTest passed");
    }

    /** Verifies cent rounding on storage and converted amount rounding on retrieval. */
    static void storesRoundedAmountAndConverts() throws Exception {
        var file = Files.createTempFile("purchases", ".jsonl");
        ExchangeRateClient client = (currency, purchaseDate, earliest) -> Optional.of(new ExchangeRate("Canada-Dollar", new BigDecimal("1.3690"), LocalDate.parse("2025-12-31")));
        var service = new PurchaseService(new FilePurchaseRepository(file), client);

        var purchase = service.store("Coffee", "2026-01-15", "10.005");
        assertEquals(new BigDecimal("10.01"), purchase.purchaseAmountUsd(), "rounded purchase amount");
        var converted = service.convert(purchase.id(), "Canada-Dollar");
        assertEquals(new BigDecimal("13.70"), converted.convertedAmount(), "converted amount");
    }

    /** Verifies the product brief's 50-character description limit. */
    static void rejectsInvalidDescription() throws Exception {
        var file = Files.createTempFile("purchases", ".jsonl");
        var service = new PurchaseService(new FilePurchaseRepository(file), (c, d, e) -> Optional.empty());
        assertThrows(ValidationException.class, () -> service.store("x".repeat(51), "2026-01-01", "1.00"));
    }

    /** Verifies a missing six-month Treasury rate is reported as an error. */
    static void rejectsNoRateWithinSixMonths() throws Exception {
        var file = Files.createTempFile("purchases", ".jsonl");
        var service = new PurchaseService(new FilePurchaseRepository(file), (c, d, e) -> Optional.empty());
        var purchase = service.store("Book", "2026-01-01", "15.00");
        assertThrows(RuntimeException.class, () -> service.convert(purchase.id(), "Canada-Dollar"));
    }

    /** Tiny assertion helper to avoid adding an external test framework dependency. */
    static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + " expected " + expected + " but got " + actual);
    }

    /** Tiny exception assertion helper to keep the repository self-contained. */
    static void assertThrows(Class<? extends Throwable> type, Runnable runnable) {
        try { runnable.run(); }
        catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("Expected " + type.getName() + " but got " + t.getClass().getName(), t);
        }
        throw new AssertionError("Expected exception: " + type.getName());
    }
}
