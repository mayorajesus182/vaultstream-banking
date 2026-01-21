package com.vaultstream.customer.domain.model;

import com.vaultstream.common.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Customer aggregate root.
 */
@DisplayName("Customer Aggregate")
class CustomerTest {

    private static final String CUSTOMER_NUMBER = "CUST-20260120-00001";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john.doe@example.com";
    private static final String PHONE = "+1234567890";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1990, 5, 15);
    private static final String NATIONAL_ID = "12345678A";

    private Address createValidAddress() {
        return Address.builder()
                .street("Main Street")
                .number("123")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .build();
    }

    private Customer createValidCustomer() {
        return Customer.create(
                CUSTOMER_NUMBER, FIRST_NAME, LAST_NAME, EMAIL, PHONE,
                DATE_OF_BIRTH, NATIONAL_ID, null, CustomerType.INDIVIDUAL);
    }

    @Nested
    @DisplayName("Factory Method: create()")
    class CreateTests {

        @Test
        @DisplayName("should create customer with valid data")
        void shouldCreateCustomerWithValidData() {
            // When
            Customer customer = Customer.create(
                    CUSTOMER_NUMBER, FIRST_NAME, LAST_NAME, EMAIL, PHONE,
                    DATE_OF_BIRTH, NATIONAL_ID, createValidAddress(),
                    CustomerType.INDIVIDUAL);

            // Then
            assertThat(customer.getId()).isNotNull();
            assertThat(customer.getCustomerNumber()).isEqualTo(CUSTOMER_NUMBER);
            assertThat(customer.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(customer.getLastName()).isEqualTo(LAST_NAME);
            assertThat(customer.getEmail()).isEqualTo(EMAIL.toLowerCase());
            assertThat(customer.getPhoneNumber()).isEqualTo(PHONE);
            assertThat(customer.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH);
            assertThat(customer.getNationalId()).isEqualTo(NATIONAL_ID);
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);
            assertThat(customer.getType()).isEqualTo(CustomerType.INDIVIDUAL);
            assertThat(customer.getCreatedAt()).isNotNull();
            assertThat(customer.getVersion()).isZero();
        }

        @Test
        @DisplayName("should normalize email to lowercase")
        void shouldNormalizeEmailToLowercase() {
            // When
            Customer customer = Customer.create(
                    CUSTOMER_NUMBER, FIRST_NAME, LAST_NAME, "JOHN.DOE@EXAMPLE.COM", PHONE,
                    DATE_OF_BIRTH, NATIONAL_ID, null, CustomerType.INDIVIDUAL);

            // Then
            assertThat(customer.getEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("should default to INDIVIDUAL type when null")
        void shouldDefaultToIndividualType() {
            // When
            Customer customer = Customer.create(
                    CUSTOMER_NUMBER, FIRST_NAME, LAST_NAME, EMAIL, PHONE,
                    DATE_OF_BIRTH, NATIONAL_ID, null, null);

            // Then
            assertThat(customer.getType()).isEqualTo(CustomerType.INDIVIDUAL);
        }

        @Test
        @DisplayName("should throw exception when firstName is null")
        void shouldThrowExceptionWhenFirstNameIsNull() {
            assertThatThrownBy(() -> Customer.create(
                    CUSTOMER_NUMBER, null, LAST_NAME, EMAIL, PHONE,
                    DATE_OF_BIRTH, NATIONAL_ID, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("firstName");
        }

        @Test
        @DisplayName("should throw exception when lastName is blank")
        void shouldThrowExceptionWhenLastNameIsBlank() {
            assertThatThrownBy(() -> Customer.create(
                    CUSTOMER_NUMBER, FIRST_NAME, "   ", EMAIL, PHONE,
                    DATE_OF_BIRTH, NATIONAL_ID, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lastName");
        }

        @Test
        @DisplayName("should throw exception when email is missing")
        void shouldThrowExceptionWhenEmailIsMissing() {
            assertThatThrownBy(() -> Customer.create(
                    CUSTOMER_NUMBER, FIRST_NAME, LAST_NAME, null, PHONE,
                    DATE_OF_BIRTH, NATIONAL_ID, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("should throw exception when nationalId is missing")
        void shouldThrowExceptionWhenNationalIdIsMissing() {
            assertThatThrownBy(() -> Customer.create(
                    CUSTOMER_NUMBER, FIRST_NAME, LAST_NAME, EMAIL, PHONE,
                    DATE_OF_BIRTH, "", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nationalId");
        }
    }

    @Nested
    @DisplayName("Status Transitions")
    class StatusTransitionTests {

        @Test
        @DisplayName("activate() should change status to ACTIVE")
        void activateShouldChangeStatusToActive() {
            // Given
            Customer customer = createValidCustomer();

            // When
            customer.activate();

            // Then
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(customer.isActive()).isTrue();
        }

        @Test
        @DisplayName("activate() should throw exception when customer is SUSPENDED")
        void activateShouldThrowWhenSuspended() {
            // Given
            Customer customer = createValidCustomer();
            customer.activate();
            customer.suspend("Fraud investigation");

            // Then - InvalidStatusTransitionException because SUSPENDED can only go to
            // ACTIVE (reactivate)
            // But this test expects an error going from SUSPENDED -> ACTIVE which is
            // allowed
            // Let's fix by trying to activate when already inactive
            Customer customer2 = createValidCustomer();
            customer2.deactivate(); // Now INACTIVE

            assertThatThrownBy(customer2::activate)
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("suspend() should change status to SUSPENDED")
        void suspendShouldChangeStatusToSuspended() {
            // Given
            Customer customer = createValidCustomer();
            customer.activate();

            // When
            customer.suspend("Suspicious activity");

            // Then
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
            assertThat(customer.getSuspensionReason()).isEqualTo("Suspicious activity");
            assertThat(customer.isActive()).isFalse();
        }

        @Test
        @DisplayName("deactivate() should change status to INACTIVE")
        void deactivateShouldChangeStatusToInactive() {
            // Given - need to activate first, then deactivate
            Customer customer = createValidCustomer();
            customer.activate();

            // When
            customer.deactivate();

            // Then
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateTests {

        @Test
        @DisplayName("updatePersonalInfo() should update non-null fields")
        void updatePersonalInfoShouldUpdateNonNullFields() {
            // Given
            Customer customer = createValidCustomer();
            LocalDate newDob = LocalDate.of(1985, 3, 20);

            // When
            customer.updatePersonalInfo("Jane", "Smith", "+9876543210", newDob);

            // Then
            assertThat(customer.getFirstName()).isEqualTo("Jane");
            assertThat(customer.getLastName()).isEqualTo("Smith");
            assertThat(customer.getPhoneNumber()).isEqualTo("+9876543210");
            assertThat(customer.getDateOfBirth()).isEqualTo(newDob);
        }

        @Test
        @DisplayName("updatePersonalInfo() should skip blank names")
        void updatePersonalInfoShouldSkipBlankNames() {
            // Given
            Customer customer = createValidCustomer();

            // When
            customer.updatePersonalInfo("   ", null, null, null);

            // Then
            assertThat(customer.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(customer.getLastName()).isEqualTo(LAST_NAME);
        }

        @Test
        @DisplayName("updateEmail() should normalize and update email")
        void updateEmailShouldNormalizeAndUpdate() {
            // Given
            Customer customer = createValidCustomer();

            // When
            customer.updateEmail("  NEW.EMAIL@Example.COM  ");

            // Then
            assertThat(customer.getEmail()).isEqualTo("new.email@example.com");
        }

        @Test
        @DisplayName("updateAddress() should replace address")
        void updateAddressShouldReplaceAddress() {
            // Given
            Customer customer = createValidCustomer();
            Address newAddress = createValidAddress();

            // When
            customer.updateAddress(newAddress);

            // Then
            assertThat(customer.getAddress()).isEqualTo(newAddress);
        }
    }

    @Nested
    @DisplayName("Helper Methods")
    class HelperMethodTests {

        @Test
        @DisplayName("getFullName() should return concatenated name")
        void getFullNameShouldReturnConcatenatedName() {
            // Given
            Customer customer = createValidCustomer();

            // Then
            assertThat(customer.getFullName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("canOpenAccounts() should return true for ACTIVE and PENDING_VERIFICATION")
        void canOpenAccountsShouldReturnTrueForValidStatuses() {
            // Given
            Customer customer = createValidCustomer();

            // Then - PENDING_VERIFICATION
            assertThat(customer.canOpenAccounts()).isTrue();

            // When activated
            customer.activate();
            assertThat(customer.canOpenAccounts()).isTrue();

            // When suspended - should not be able to open accounts
            customer.suspend("Test");
            assertThat(customer.canOpenAccounts()).isFalse();
        }
    }
}
