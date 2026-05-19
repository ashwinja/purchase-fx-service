package com.example.purchasefx.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small dependency-free command-line client for the Purchase FX Service.
 *
 * Usage examples:
 *   java -cp build/classes com.example.purchasefx.client.PurchaseFxClient create "Laptop bag" 2026-01-15 120.129
 *   java -cp build/classes com.example.purchasefx.client.PurchaseFxClient get <purchase-id>
 *   java -cp build/classes com.example.purchasefx.client.PurchaseFxClient convert <purchase-id> Canada-Dollar
 *
 * Optional base URL:
 *   PURCHASE_FX_BASE_URL=http://localhost:8080
 *   or --base-url http://localhost:8080
 */
public final class PurchaseFxClient {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private final HttpClient httpClient;
    private final String baseUrl;

    private PurchaseFxClient(String baseUrl) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /** Parses CLI arguments and dispatches to the requested real HTTP operation. */
    public static void main(String[] args) throws Exception {
        ParsedArgs parsed = parseArgs(args);
        if (parsed.command == null || parsed.command.equals("help") || parsed.command.equals("--help") || parsed.command.equals("-h")) {
            printUsage();
            return;
        }

        var client = new PurchaseFxClient(parsed.baseUrl);
        switch (parsed.command) {
            case "create" -> client.create(parsed.remaining);
            case "get" -> client.get(parsed.remaining);
            case "convert" -> client.convert(parsed.remaining);
            default -> {
                System.err.println("Unknown command: " + parsed.command);
                printUsage();
                System.exit(2);
            }
        }
    }

    /** Calls POST /api/purchases on the running service using the JDK HTTP client. */
    private void create(String[] args) throws IOException, InterruptedException {
        requireArgCount("create", args, 3);
        String description = args[0];
        String transactionDate = args[1];
        String purchaseAmount = new BigDecimal(args[2]).toPlainString();

        String requestBody = "{"
                + "\"description\":\"" + jsonEscape(description) + "\","
                + "\"transactionDate\":\"" + jsonEscape(transactionDate) + "\","
                + "\"purchaseAmount\":\"" + jsonEscape(purchaseAmount) + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/purchases"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        sendAndPrint(request);
    }

    /** Calls GET /api/purchases/{id} on the running service. */
    private void get(String[] args) throws IOException, InterruptedException {
        requireArgCount("get", args, 1);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/purchases/" + encodePath(args[0])))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        sendAndPrint(request);
    }

    /** Calls GET /api/purchases/{id}/conversion on the running service. */
    private void convert(String[] args) throws IOException, InterruptedException {
        requireArgCount("convert", args, 2);
        String id = args[0];
        String currency = args[1];

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/purchases/" + encodePath(id)
                        + "/conversion?currency=" + encodeQuery(currency)))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        sendAndPrint(request);
    }

    /** Sends the HTTP request, pretty-prints the response, and exits non-zero on API errors. */
    private void sendAndPrint(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("HTTP " + response.statusCode());
        System.out.println(prettyJson(response.body()));

        if (response.statusCode() >= 400) {
            System.exit(1);
        }
    }

    /** Prints command-line help for reviewers running without an IDE or Postman. */
    private static void printUsage() {
        System.out.println("""
                Purchase FX Java Client

                Usage:
                  ./scripts/client.sh create  <description> <transaction-date-yyyy-MM-dd> <purchase-amount-usd>
                  ./scripts/client.sh get     <purchase-id>
                  ./scripts/client.sh convert <purchase-id> <treasury-country-currency-desc>

                Options:
                  --base-url <url>   Override the service base URL. Default: http://localhost:8080

                Environment:
                  PURCHASE_FX_BASE_URL=http://localhost:8080

                Examples:
                  ./scripts/client.sh create "Laptop bag" 2026-01-15 120.129
                  ./scripts/client.sh get d0d05b4f-44f8-4f0e-8e5f-c4f6d2d4968e
                  ./scripts/client.sh convert d0d05b4f-44f8-4f0e-8e5f-c4f6d2d4968e Canada-Dollar
                """);
    }

    /** Supports --base-url and PURCHASE_FX_BASE_URL while leaving command parsing simple. */
    private static ParsedArgs parseArgs(String[] args) {
        String baseUrl = System.getenv().getOrDefault("PURCHASE_FX_BASE_URL", DEFAULT_BASE_URL);
        int index = 0;
        if (args.length >= 2 && args[0].equals("--base-url")) {
            baseUrl = args[1];
            index = 2;
        }
        String command = index < args.length ? args[index] : null;
        String[] remaining = java.util.Arrays.copyOfRange(args, Math.min(index + 1, args.length), args.length);
        return new ParsedArgs(baseUrl, command, remaining);
    }

    /** Fails fast when a command receives too few or too many arguments. */
    private static void requireArgCount(String command, String[] args, int expected) {
        if (args.length != expected) {
            System.err.printf("Command '%s' expected %d argument(s), but received %d.%n", command, expected, args.length);
            printUsage();
            System.exit(2);
        }
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Small JSON pretty-printer so CLI responses are readable without external tools. */
    private static String prettyJson(String json) {
        if (json == null || json.isBlank()) return "";
        StringBuilder out = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '"' && (i == 0 || json.charAt(i - 1) != '\\')) inString = !inString;
            if (!inString && (ch == '{' || ch == '[')) {
                out.append(ch).append('\n');
                indent++;
                appendIndent(out, indent);
            } else if (!inString && (ch == '}' || ch == ']')) {
                out.append('\n');
                indent--;
                appendIndent(out, indent);
                out.append(ch);
            } else if (!inString && ch == ',') {
                out.append(ch).append('\n');
                appendIndent(out, indent);
            } else if (!inString && ch == ':') {
                out.append(": ");
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static void appendIndent(StringBuilder out, int indent) {
        out.append("  ".repeat(Math.max(0, indent)));
    }

    /** Parsed command-line arguments after optional global flags are removed. */
    private record ParsedArgs(String baseUrl, String command, String[] remaining) {}
}
