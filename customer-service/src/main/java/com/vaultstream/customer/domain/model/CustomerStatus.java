package com.vaultstream.customer.domain.model;

/**
 * Customer status enum
 */
public enum CustomerStatus {

    /**
     * Customer registration is pending verification
     */
    PENDING_VERIFICATION("Pending Verification"),

    /**
     * Customer is verified and active
     */
    ACTIVE("Active"),

    /**
     * Customer is temporarily suspended
     */
    SUSPENDED("Suspended"),

    /**
     * Customer account is deactivated
     */
    INACTIVE("Inactive");

    private final String displayName;

    CustomerStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
