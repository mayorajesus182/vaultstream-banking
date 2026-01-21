package com.vaultstream.common.exception;

/**
 * Exception thrown when an invalid email format is provided.
 */
public class InvalidEmailException extends VaultStreamException {

    public InvalidEmailException(String email) {
        super("INVALID_EMAIL", "Invalid email format: " + email);
    }

    public InvalidEmailException(String message, String email) {
        super("INVALID_EMAIL", message + ": " + email);
    }
}
