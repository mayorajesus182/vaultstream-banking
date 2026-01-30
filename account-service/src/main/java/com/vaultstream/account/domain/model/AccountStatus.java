package com.vaultstream.account.domain.model;

/**
 * Enum representing the possible states of a bank account.
 */
public enum AccountStatus {
    /**
     * Account is pending activation
     */
    PENDING,

    /**
     * Account is active and operational
     */
    ACTIVE,

    /**
     * Account is temporarily frozen (no transactions allowed)
     */
    FROZEN,

    /**
     * Account is permanently closed
     */
    CLOSED
}
