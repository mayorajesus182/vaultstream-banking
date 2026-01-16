package com.vaultstream.common.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends VaultStreamException {

    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(ERROR_CODE, String.format("%s not found with ID: %s", resourceType, resourceId));
    }

    public ResourceNotFoundException(String message) {
        super(ERROR_CODE, message);
    }
}
