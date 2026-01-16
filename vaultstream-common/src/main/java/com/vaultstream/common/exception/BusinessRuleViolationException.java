package com.vaultstream.common.exception;

/**
 * Exception thrown when a business rule is violated.
 */
public class BusinessRuleViolationException extends VaultStreamException {

    private static final String ERROR_CODE = "BUSINESS_RULE_VIOLATION";

    public BusinessRuleViolationException(String message) {
        super(ERROR_CODE, message);
    }

    public BusinessRuleViolationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
