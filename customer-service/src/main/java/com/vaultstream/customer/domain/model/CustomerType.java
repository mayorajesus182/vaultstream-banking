package com.vaultstream.customer.domain.model;

/**
 * Type of customer
 */
public enum CustomerType {

    /**
     * Individual person
     */
    INDIVIDUAL("Individual"),

    /**
     * Business entity
     */
    BUSINESS("Business"),

    /**
     * Premium customer with special benefits
     */
    PREMIUM("Premium");

    private final String displayName;

    CustomerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
