package com.vaultstream.common.exception;

import lombok.Getter;

/**
 * Exception thrown when a customer does not meet the minimum age requirement.
 */
@Getter
public class MinimumAgeRequiredException extends VaultStreamException {

    private final int currentAge;
    private final int minimumAge;

    public MinimumAgeRequiredException(int currentAge, int minimumAge) {
        super("MINIMUM_AGE_REQUIRED",
                String.format("Customer must be at least %d years old. Current age: %d", minimumAge, currentAge));
        this.currentAge = currentAge;
        this.minimumAge = minimumAge;
    }

    public MinimumAgeRequiredException(String message, int currentAge, int minimumAge) {
        super("MINIMUM_AGE_REQUIRED", message);
        this.currentAge = currentAge;
        this.minimumAge = minimumAge;
    }
}
