package com.vaultstream.common.exception;

/**
 * Exception thrown when an invalid national ID format is provided.
 */
public class InvalidNationalIdException extends VaultStreamException {

    public InvalidNationalIdException(String nationalId) {
        super("INVALID_NATIONAL_ID", "Invalid national ID format: " + nationalId);
    }

    public InvalidNationalIdException(String message, String nationalId) {
        super("INVALID_NATIONAL_ID", message + ": " + nationalId);
    }
}
