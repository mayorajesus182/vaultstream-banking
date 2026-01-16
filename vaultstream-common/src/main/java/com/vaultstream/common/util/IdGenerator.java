package com.vaultstream.common.util;

import java.util.UUID;

/**
 * Utility class for generating IDs.
 */
public final class IdGenerator {

    private IdGenerator() {
        // Utility class
    }

    /**
     * Generate a new UUID as string
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a new UUID
     */
    public static UUID generateUUID() {
        return UUID.randomUUID();
    }

    /**
     * Parse a string to UUID
     */
    public static UUID parse(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID cannot be null or blank");
        }
        return UUID.fromString(id);
    }

    /**
     * Check if a string is a valid UUID
     */
    public static boolean isValid(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
