package com.vaultstream.common.exception;

/**
 * Exception for optimistic locking conflicts in event sourcing.
 */
public class ConcurrencyException extends VaultStreamException {

    private static final String ERROR_CODE = "CONCURRENCY_CONFLICT";

    public ConcurrencyException(String aggregateId, int expectedVersion, int actualVersion) {
        super(ERROR_CODE, String.format(
                "Concurrency conflict for aggregate %s. Expected version: %d, Actual version: %d",
                aggregateId, expectedVersion, actualVersion));
    }

    public ConcurrencyException(String message) {
        super(ERROR_CODE, message);
    }
}
