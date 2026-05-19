package com.example.purchasefx.infra;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Minimal JSON helper used to keep this take-home project dependency-free.
 *
 * <p>The service accepts and returns a deliberately small JSON shape, so these helpers
 * are sufficient for the controlled payloads used by the API and tests. In a larger
 * production service, this would normally be replaced with a mature JSON library such
 * as Jackson.</p>
 */
public final class Json {
    private Json() {
        // Static helper class.
    }

    /**
     * Extracts a string value from a flat JSON object.
     *
     * <p>The parser handles escaped characters in string values and throws a clear
     * {@link IllegalArgumentException} when a required key is missing or malformed.</p>
     */
    public static String stringValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex < 0) throw new IllegalArgumentException("Missing JSON key: " + key);
        int colon = json.indexOf(':', keyIndex + pattern.length());
        int firstQuote = json.indexOf('"', colon + 1);
        int i = firstQuote + 1;
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (escaped) {
                sb.append(switch (c) {
                    case '"' -> '"';
                    case '\\' -> '\\';
                    case '/' -> '/';
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> c;
                });
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        throw new IllegalArgumentException("Unterminated string value for JSON key: " + key);
    }

    /** Escapes a Java string so it can be safely embedded as a JSON string value. */
    public static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Builds the standard API error response shape. */
    public static String error(String code, String message) {
        return "{\"error\":{\"code\":\"" + escape(code) + "\",\"message\":\"" + escape(message) + "\"}}";
    }

    /** Builds the standard purchase response shape. */
    public static String purchase(String id, String description, LocalDate transactionDate, BigDecimal purchaseAmountUsd) {
        return "{"
                + "\"id\":\"" + id + "\","
                + "\"description\":\"" + escape(description) + "\","
                + "\"transactionDate\":\"" + transactionDate + "\","
                + "\"purchaseAmountUsd\":" + purchaseAmountUsd
                + "}";
    }
}
