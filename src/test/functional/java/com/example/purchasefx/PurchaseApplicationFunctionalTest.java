package com.example.purchasefx;

import com.example.purchasefx.api.PurchaseHttpHandler;
import com.example.purchasefx.infra.FilePurchaseRepository;
import com.example.purchasefx.service.PurchaseService;
import com.sun.net.httpserver.HttpServer;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * End-to-end functional test for the HTTP API.
 *
 * <p>The test starts the embedded HTTP server on a random local port, sends real HTTP
 * requests, and verifies the full create-and-convert flow. It supplies a
 * deterministic in-memory exchange-rate client so the functional test remains
 * repeatable, while the actual application and Java CLI client still use real HTTP
 * integrations at runtime.</p>
 */
public final class PurchaseApplicationFunctionalTest {
    /** Runs the functional API test from the command-line test script. */
    public static void main(String[] args) throws Exception {
        var file = Files.createTempFile("purchases", ".jsonl");
        var service = new PurchaseService(new FilePurchaseRepository(file), (currency, purchaseDate, earliest) -> Optional.of(
                new com.example.purchasefx.domain.ExchangeRate("Euro Zone-Euro", new BigDecimal("0.8510"), LocalDate.parse("2025-12-31"))));
        var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/purchases", new PurchaseHttpHandler(service));
        var executor = Executors.newFixedThreadPool(2);
        server.setExecutor(executor);
        server.start();
        try {
            var base = "http://localhost:" + server.getAddress().getPort();
            var client = HttpClient.newHttpClient();
            var createReq = HttpRequest.newBuilder(URI.create(base + "/api/purchases"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"description\":\"Laptop bag\",\"transactionDate\":\"2026-01-15\",\"purchaseAmount\":\"120.129\"}"))
                    .build();
            var createResp = client.send(createReq, HttpResponse.BodyHandlers.ofString());
            if (createResp.statusCode() != 201) throw new AssertionError("create status " + createResp.statusCode() + " " + createResp.body());
            var id = value(createResp.body(), "id");
            var convertReq = HttpRequest.newBuilder(URI.create(base + "/api/purchases/" + id + "/conversion?currency=Euro%20Zone-Euro")).GET().build();
            var convertResp = client.send(convertReq, HttpResponse.BodyHandlers.ofString());
            if (convertResp.statusCode() != 200) throw new AssertionError("convert status " + convertResp.statusCode() + " " + convertResp.body());
            if (!convertResp.body().contains("\"convertedAmount\":102.23")) throw new AssertionError(convertResp.body());
            System.out.println("PurchaseApplicationFunctionalTest passed");
        } finally {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    /** Reads a string property from the JSON response using the project JSON helper. */
    private static String value(String json, String key) {
        return com.example.purchasefx.infra.Json.stringValue(json, key);
    }
}
