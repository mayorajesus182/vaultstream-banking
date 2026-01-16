package com.vaultstream.customer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer Aggregate Root.
 * 
 * Represents a bank customer with personal information and contact details.
 * This is the core domain entity that encapsulates business rules.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    private UUID id;
    private String customerNumber; // Unique business identifier
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String nationalId; // Cédula, DNI, SSN, etc.
    private Address address;
    private CustomerStatus status;
    private CustomerType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version; // For optimistic locking

    /**
     * Factory method to create a new customer
     */
    public static Customer create(
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            LocalDate dateOfBirth,
            String nationalId,
            Address address,
            CustomerType type) {

        validateRequired(firstName, "firstName");
        validateRequired(lastName, "lastName");
        validateRequired(email, "email");
        validateRequired(nationalId, "nationalId");

        return Customer.builder()
                .id(UUID.randomUUID())
                .customerNumber(generateCustomerNumber())
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .email(email.toLowerCase().trim())
                .phoneNumber(phoneNumber)
                .dateOfBirth(dateOfBirth)
                .nationalId(nationalId.trim())
                .address(address)
                .status(CustomerStatus.PENDING_VERIFICATION)
                .type(type != null ? type : CustomerType.INDIVIDUAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(0)
                .build();
    }

    /**
     * Update customer personal information
     */
    public void updatePersonalInfo(String firstName, String lastName, String phoneNumber, LocalDate dateOfBirth) {
        if (firstName != null && !firstName.isBlank()) {
            this.firstName = firstName.trim();
        }
        if (lastName != null && !lastName.isBlank()) {
            this.lastName = lastName.trim();
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
        if (dateOfBirth != null) {
            this.dateOfBirth = dateOfBirth;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update customer address
     */
    public void updateAddress(Address newAddress) {
        this.address = newAddress;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update email (requires verification)
     */
    public void updateEmail(String email) {
        validateRequired(email, "email");
        this.email = email.toLowerCase().trim();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Activate customer after verification
     */
    public void activate() {
        if (this.status == CustomerStatus.SUSPENDED) {
            throw new IllegalStateException("Cannot activate a suspended customer");
        }
        this.status = CustomerStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Suspend customer (e.g., for fraud investigation)
     */
    public void suspend(String reason) {
        this.status = CustomerStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Deactivate customer (soft delete)
     */
    public void deactivate() {
        this.status = CustomerStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Check if customer is active
     */
    public boolean isActive() {
        return this.status == CustomerStatus.ACTIVE;
    }

    /**
     * Check if customer can open accounts
     */
    public boolean canOpenAccounts() {
        return this.status == CustomerStatus.ACTIVE || this.status == CustomerStatus.PENDING_VERIFICATION;
    }

    // ========================================
    // Private helpers
    // ========================================

    private static void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private static String generateCustomerNumber() {
        // Format: CUST-YYYYMMDD-XXXXX
        String datePart = LocalDate.now().toString().replace("-", "");
        String randomPart = String.format("%05d", (int) (Math.random() * 100000));
        return "CUST-" + datePart + "-" + randomPart;
    }

    /**
     * Set ID (used by persistence layer)
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Set version (used for optimistic locking)
     */
    public void setVersion(int version) {
        this.version = version;
    }
}
