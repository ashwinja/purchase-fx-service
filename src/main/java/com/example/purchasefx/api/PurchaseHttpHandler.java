package com.example.purchasefx.api;

import com.example.purchasefx.domain.CurrencyConversionException;
import com.example.purchasefx.domain.NotFoundException;
import com.example.purchasefx.domain.ValidationException;
import com.example.purchasefx.infra.Json;
import com.example.purchasefx.service.PurchaseService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * HTTP adapter for the purchase REST API.
 *
 * <p>This class is deliberately thin: it handles routing, request/response JSON mapping,
 * HTTP status codes, and exception-to-error translation. Business rules stay in
 * {@link PurchaseService} so the domain can be tested without running a web server.</p>
 */
public final class PurchaseHttpHandler implements HttpHandler {
    private final PurchaseService purchaseService;

    public PurchaseHttpHandler(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    /**
     * Routes supported API requests:
     * <ul>
     *   <li>{@code POST /api/purchases}</li>
     *   <li>{@code GET /api/purchases/{id}}</li>
     *   <li>{@code GET /api/purchases/{id}/conversion?currency=Canada-Dollar}</li>
     * </ul>
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            addCommonHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 204, "");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if ("POST".equals(exchange.getRequestMethod()) && "/api/purchases".equals(path)) {
                createPurchase(exchange);
                return;
            }
            if ("GET".equals(exchange.getRequestMethod()) && path.matches("/api/purchases/[^/]+")) {
                getPurchase(exchange, path.substring("/api/purchases/".length()));
                return;
            }
            if ("GET".equals(exchange.getRequestMethod()) && path.matches("/api/purchases/[^/]+/conversion")) {
                var id = path.substring("/api/purchases/".length(), path.length() - "/conversion".length());
                convertPurchase(exchange, id);
                return;
            }

            send(exchange, 404, Json.error("NOT_FOUND", "Route not found"));
        } catch (ValidationException e) {
            send(exchange, 400, Json.error("VALIDATION_ERROR", e.getMessage()));
        } catch (NotFoundException e) {
            send(exchange, 404, Json.error("NOT_FOUND", e.getMessage()));
        } catch (CurrencyConversionException e) {
            send(exchange, 422, Json.error("CURRENCY_CONVERSION_UNAVAILABLE", e.getMessage()));
        } catch (IllegalArgumentException e) {
            send(exchange, 400, Json.error("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            send(exchange, 500, Json.error("INTERNAL_SERVER_ERROR", "Unexpected server error"));
        }
    }

    /** Reads the create request JSON, delegates validation/storage, and returns HTTP 201. */
    private void createPurchase(HttpExchange exchange) throws IOException {
        var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        var description = Json.stringValue(body, "description");
        var transactionDate = Json.stringValue(body, "transactionDate");
        var purchaseAmount = Json.stringValue(body, "purchaseAmount");
        var purchase = purchaseService.store(description, transactionDate, purchaseAmount);
        send(exchange, 201, Json.purchase(purchase.id().toString(), purchase.description(), purchase.transactionDate(), purchase.purchaseAmountUsd()));
    }

    /** Returns a previously stored purchase by UUID. */
    private void getPurchase(HttpExchange exchange, String idText) throws IOException {
        var p = purchaseService.get(UUID.fromString(idText));
        send(exchange, 200, Json.purchase(p.id().toString(), p.description(), p.transactionDate(), p.purchaseAmountUsd()));
    }

    /** Returns the stored purchase converted to the requested Treasury currency description. */
    private void convertPurchase(HttpExchange exchange, String idText) throws IOException {
        var currency = queryParameter(exchange, "currency");
        var c = purchaseService.convert(UUID.fromString(idText), currency);
        send(exchange, 200, "{"
                + "\"id\":\"" + c.id() + "\","
                + "\"description\":\"" + Json.escape(c.description()) + "\","
                + "\"transactionDate\":\"" + c.transactionDate() + "\","
                + "\"purchaseAmountUsd\":" + c.purchaseAmountUsd() + ","
                + "\"targetCurrency\":\"" + Json.escape(c.targetCurrency()) + "\","
                + "\"exchangeRateUsed\":" + c.exchangeRateUsed() + ","
                + "\"exchangeRateDate\":\"" + c.exchangeRateDate() + "\","
                + "\"convertedAmount\":" + c.convertedAmount().setScale(2)
                + "}");
    }

    /** Extracts and URL-decodes one query-string parameter from the incoming request. */
    private static String queryParameter(HttpExchange exchange, String name) {
        var query = exchange.getRequestURI().getRawQuery();
        if (query == null) return null;
        for (String part : query.split("&")) {
            var pieces = part.split("=", 2);
            if (pieces.length == 2 && pieces[0].equals(name)) {
                return java.net.URLDecoder.decode(pieces[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /** Adds JSON and CORS headers for all API responses. */
    private static void addCommonHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    /** Sends a fully buffered JSON response and closes the response stream. */
    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var responseBody = exchange.getResponseBody()) {
            responseBody.write(bytes);
        }
    }
}
