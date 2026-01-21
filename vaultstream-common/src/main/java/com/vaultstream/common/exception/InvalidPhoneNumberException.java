package com.vaultstream.common.exception;

/**
 * Exception thrown when an invalid phone number format is provided.
 */
public class InvalidPhoneNumberException extends VaultStreamException {

    public InvalidPhoneNumberException(String phoneNumber) {
        super("INVALID_PHONE_NUMBER", "Invalid phone number format: " + phoneNumber);
    }

    public InvalidPhoneNumberException(String message, String phoneNumber) {
        super("INVALID_PHONE_NUMBER", message + ": " + phoneNumber);
    }
}
