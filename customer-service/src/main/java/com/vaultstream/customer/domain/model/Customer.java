package com.vaultstream.customer.domain.model;

import com.vaultstream.common.exception.InvalidEmailException;
import com.vaultstream.common.exception.InvalidPhoneNumberException;
import com.vaultstream.common.exception.InvalidStatusTransitionException;
import com.vaultstream.common.exception.MinimumAgeRequiredException;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Customer Aggregate Root.
 * 
 * Represents a bank customer with personal information and contact details.
 * This is the core domain entity that encapsulates business rules.
 */
@Getter
public class Customer {

    // Email validation pattern (RFC 5322 simplified)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Phone number pattern (E.164 format: +[country code][number])
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[1-9]\\d{1,14}$");

    private static final int MINIMUM_AGE = 18;
    private static final int MAXIMUM_AGE = 120;

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
    private String suspensionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version; // For optimistic locking

    /**
     * Valid status transitions for customer lifecycle.
     */
    public enum StatusTransition {
        PENDING_TO_ACTIVE(CustomerStatus.PENDING_VERIFICATION, CustomerStatus.ACTIVE),
        ACTIVE_TO_SUSPENDED(CustomerStatus.ACTIVE, CustomerStatus.SUSPENDED),
        SUSPENDED_TO_ACTIVE(CustomerStatus.SUSPENDED, CustomerStatus.ACTIVE),
        ACTIVE_TO_INACTIVE(CustomerStatus.ACTIVE, CustomerStatus.INACTIVE),
        PENDING_TO_INACTIVE(CustomerStatus.PENDING_VERIFICATION, CustomerStatus.INACTIVE);

        private final CustomerStatus from;
        private final CustomerStatus to;

        StatusTransition(CustomerStatus from, CustomerStatus to) {
            this.from = from;
            this.to = to;
        }

        public static boolean isValid(CustomerStatus from, CustomerStatus to) {
            return Arrays.stream(values())
                    .anyMatch(t -> t.from == from && t.to == to);
        }

        public static String getAllowedTransitions(CustomerStatus from) {
            return Arrays.stream(values())
                    .filter(t -> t.from == from)
                    .map(t -> t.to.toString())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none");
        }
    }

    /**
     * Private constructor - use factory method create()
     */
    private Customer(UUID id, String customerNumber, String firstName, String lastName,
            String email, String phoneNumber, LocalDate dateOfBirth, String nationalId,
            Address address, CustomerStatus status, CustomerType type,
            LocalDateTime createdAt, LocalDateTime updatedAt, int version) {
        this.id = id;
        this.customerNumber = customerNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
        this.nationalId = nationalId;
        this.address = address;
        this.status = status;
        this.type = type;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    /**
     * Factory method to create a new customer
     */
    public static Customer create(
            String customerNumber,
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            LocalDate dateOfBirth,
            String nationalId,
            Address address,
            CustomerType type) {

        // Required field validations
        validateRequired(firstName, "firstName");
        validateRequired(lastName, "lastName");
        validateRequired(email, "email");
        validateRequired(nationalId, "nationalId");
        validateRequired(customerNumber, "customerNumber");

        // Format validations
        validateEmail(email);
        validateAge(dateOfBirth);
        validateNameLength(firstName, "firstName");
        validateNameLength(lastName, "lastName");

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            validatePhoneNumber(phoneNumber);
        }

        return new Customer(
                UUID.randomUUID(),
                customerNumber,
                firstName.trim(),
                lastName.trim(),
                email.toLowerCase().trim(),
                phoneNumber,
                dateOfBirth,
                nationalId.trim(),
                address,
                CustomerStatus.PENDING_VERIFICATION,
                type != null ? type : CustomerType.INDIVIDUAL,
                LocalDateTime.now(),
                LocalDateTime.now(),
                0);
    }

    /**
     * Update customer personal information
     */
    public void updatePersonalInfo(String firstName, String lastName, String phoneNumber, LocalDate dateOfBirth) {
        if (firstName != null && !firstName.isBlank()) {
            validateNameLength(firstName, "firstName");
            this.firstName = firstName.trim();
        }
        if (lastName != null && !lastName.isBlank()) {
            validateNameLength(lastName, "lastName");
            this.lastName = lastName.trim();
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            validatePhoneNumber(phoneNumber);
            this.phoneNumber = phoneNumber;
        }
        if (dateOfBirth != null) {
            validateAge(dateOfBirth);
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
        if (email != null)
            email = email.trim();
        validateRequired(email, "email");
        validateEmail(email);
        this.email = email.toLowerCase();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Activate customer after verification
     */
    public void activate() {
        // Idempotent - if already active, do nothing
        if (this.status == CustomerStatus.ACTIVE) {
            return;
        }

        if (!StatusTransition.isValid(this.status, CustomerStatus.ACTIVE)) {
            throw new InvalidStatusTransitionException(
                    this.status,
                    CustomerStatus.ACTIVE,
                    String.format("Cannot activate customer from status %s. Allowed transitions: %s",
                            this.status, StatusTransition.getAllowedTransitions(this.status)));
        }

        this.status = CustomerStatus.ACTIVE;
        this.suspensionReason = null; // Clear suspension reason if reactivating
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Suspend customer (e.g., for fraud investigation)
     */
    public void suspend(String reason) {
        validateRequired(reason, "suspension reason");

        if (!StatusTransition.isValid(this.status, CustomerStatus.SUSPENDED)) {
            throw new InvalidStatusTransitionException(
                    this.status,
                    CustomerStatus.SUSPENDED,
                    String.format("Cannot suspend customer from status %s. Allowed transitions: %s",
                            this.status, StatusTransition.getAllowedTransitions(this.status)));
        }

        this.status = CustomerStatus.SUSPENDED;
        this.suspensionReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Deactivate customer (soft delete)
     */
    public void deactivate() {
        if (!StatusTransition.isValid(this.status, CustomerStatus.INACTIVE)) {
            throw new InvalidStatusTransitionException(
                    this.status,
                    CustomerStatus.INACTIVE,
                    String.format("Cannot deactivate customer from status %s. Allowed transitions: %s",
                            this.status, StatusTransition.getAllowedTransitions(this.status)));
        }

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
    // Validation Methods
    // ========================================

    private static void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private static void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailException(email);
        }
    }

    private static void validateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }

        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();

        if (age < MINIMUM_AGE) {
            throw new MinimumAgeRequiredException(age, MINIMUM_AGE);
        }

        if (age > MAXIMUM_AGE) {
            throw new IllegalArgumentException(
                    "Invalid date of birth. Age cannot exceed " + MAXIMUM_AGE + " years");
        }
    }

    private static void validatePhoneNumber(String phoneNumber) {
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new InvalidPhoneNumberException(
                    "Phone number must be in E.164 format (e.g., +1234567890)",
                    phoneNumber);
        }
    }

    private static void validateNameLength(String name, String fieldName) {
        if (name.length() < 2) {
            throw new IllegalArgumentException(
                    fieldName + " must be at least 2 characters");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed 100 characters");
        }
    }

    // ========================================
    // Setters for JPA (package-private)
    // ========================================

    /**
     * Set ID (used by persistence layer)
     */
    void setId(UUID id) {
        this.id = id;
    }

    /**
     * Set version (used for optimistic locking)
     */
    void setVersion(int version) {
        this.version = version;
    }

    /**
     * Set customer number (used by persistence layer)
     */
    void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }
}
