package com.vaultstream.common.exception;

/**
 * Base exception for all VaultStream domain exceptions.
 */
public abstract class VaultStreamException extends RuntimeException {

    private final String errorCode;

    protected VaultStreamException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected VaultStreamException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
