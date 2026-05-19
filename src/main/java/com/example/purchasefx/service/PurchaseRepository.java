package com.example.purchasefx.service;

import com.example.purchasefx.domain.Purchase;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for purchase transactions.
 *
 * <p>The current implementation stores purchases in a local JSON Lines file. This
 * interface allows the storage mechanism to be changed later without rewriting the
 * HTTP or business logic layers.</p>
 */
public interface PurchaseRepository {
    /** Persists a new purchase and returns the stored object. */
    Purchase save(Purchase purchase) throws IOException;

    /** Retrieves a purchase by its unique identifier. */
    Optional<Purchase> findById(UUID id);
}
