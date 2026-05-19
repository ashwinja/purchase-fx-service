package com.example.purchasefx.infra;

import com.example.purchasefx.domain.Purchase;
import com.example.purchasefx.service.PurchaseRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File-backed purchase repository.
 *
 * <p>Purchases are persisted as one JSON object per line. JSON Lines keeps the storage
 * simple, append-only, human-readable, and durable without requiring a separate database.
 * The in-memory map provides fast lookups after the file is loaded at startup.</p>
 */
public final class FilePurchaseRepository implements PurchaseRepository {
    private final Path jsonlFile;
    private final Map<UUID, Purchase> purchases = new ConcurrentHashMap<>();

    /** Creates the storage file if needed and loads any existing purchase records. */
    public FilePurchaseRepository(Path jsonlFile) throws IOException {
        this.jsonlFile = jsonlFile;
        if (jsonlFile.getParent() != null) Files.createDirectories(jsonlFile.getParent());
        if (!Files.exists(jsonlFile)) Files.createFile(jsonlFile);
        loadExisting();
    }

    /** Appends the purchase to disk and updates the in-memory lookup map atomically. */
    @Override
    public synchronized Purchase save(Purchase purchase) throws IOException {
        purchases.put(purchase.id(), purchase);
        try (BufferedWriter writer = Files.newBufferedWriter(jsonlFile, StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND)) {
            writer.write(toJsonLine(purchase));
            writer.newLine();
        }
        return purchase;
    }

    /** Looks up a purchase by UUID from the loaded in-memory index. */
    @Override
    public Optional<Purchase> findById(UUID id) {
        return Optional.ofNullable(purchases.get(id));
    }

    /** Rebuilds the in-memory index from the JSON Lines file during application startup. */
    private void loadExisting() throws IOException {
        try (var lines = Files.lines(jsonlFile, StandardCharsets.UTF_8)) {
            lines.filter(line -> !line.isBlank()).map(FilePurchaseRepository::fromJsonLine)
                    .forEach(p -> purchases.put(p.id(), p));
        }
    }

    /** Serializes a purchase to one compact JSON object suitable for JSON Lines storage. */
    private static String toJsonLine(Purchase p) {
        return "{\"id\":\"" + p.id() + "\","
                + "\"description\":\"" + Json.escape(p.description()) + "\","
                + "\"transactionDate\":\"" + p.transactionDate() + "\","
                + "\"purchaseAmountUsd\":\"" + p.purchaseAmountUsd() + "\"}";
    }

    /** Deserializes one JSON Lines record back into a domain purchase. */
    private static Purchase fromJsonLine(String json) {
        return new Purchase(
                UUID.fromString(Json.stringValue(json, "id")),
                Json.stringValue(json, "description"),
                LocalDate.parse(Json.stringValue(json, "transactionDate")),
                new BigDecimal(Json.stringValue(json, "purchaseAmountUsd"))
        );
    }
}
