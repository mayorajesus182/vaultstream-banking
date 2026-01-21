package com.vaultstream.customer.infrastructure.persistence;

import com.vaultstream.customer.domain.model.Address;
import com.vaultstream.customer.domain.model.Customer;
import com.vaultstream.customer.domain.model.CustomerType;
import com.vaultstream.customer.domain.repository.CustomerRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for CustomerRepository using H2 (No Docker).
 */
@QuarkusTest
@DisplayName("CustomerRepository Integration")
class CustomerRepositoryIT {

    @Inject
    CustomerRepository customerRepository;

    private static int testCounter = 0;

    private Customer createTestCustomer(String email, String nationalId, String firstName, String lastName) {
        testCounter++;
        String customerNumber = "CUST-IT-H2-" + String.format("%05d", testCounter);

        Address address = Address.builder()
                .street("Test Street")
                .number("123")
                .city("Test City")
                .state("TS")
                .postalCode("12345")
                .country("USA")
                .build();

        return Customer.create(
                customerNumber,
                firstName,
                lastName,
                email,
                "+1234567890",
                LocalDate.of(1990, 1, 1),
                nationalId,
                address,
                CustomerType.INDIVIDUAL);
    }

    @Test
    @Transactional
    @DisplayName("save() should persist new customer")
    void saveShouldPersistNewCustomer() {
        // Given
        Customer customer = createTestCustomer("save.it@example.com", "SAVE-IT-001", "John", "Doe");

        // When
        Customer saved = customerRepository.save(customer);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCustomerNumber()).startsWith("CUST-IT-H2-");
    }

    @Test
    @Transactional
    @DisplayName("findById() should return customer when exists")
    void findByIdShouldReturnCustomer() {
        // Given
        Customer customer = createTestCustomer("find.it@example.com", "FIND-IT-001", "Jane", "Doe");
        Customer saved = customerRepository.save(customer);

        // When
        Optional<Customer> found = customerRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("find.it@example.com");
    }

    @Test
    @Transactional
    @DisplayName("searchByName() should find customers case-insensitive")
    void searchByNameShouldFindCustomers() {
        // Given
        Customer c1 = createTestCustomer("search1@example.com", "SEARCH-001", "Alice", "Smith");
        Customer c2 = createTestCustomer("search2@example.com", "SEARCH-002", "Bob", "Smith");
        Customer c3 = createTestCustomer("search3@example.com", "SEARCH-003", "Alice", "Wonderland");
        customerRepository.save(c1);
        customerRepository.save(c2);
        customerRepository.save(c3);

        // When
        List<Customer> smiths = customerRepository.searchByName("smith", 0, 10);
        List<Customer> alices = customerRepository.searchByName("ALICE", 0, 10);

        // Then
        assertThat(smiths).extracting(Customer::getEmail).contains("search1@example.com", "search2@example.com");
        assertThat(alices).extracting(Customer::getEmail).contains("search1@example.com", "search3@example.com");
    }

    @Test
    @Transactional
    @DisplayName("existsByCustomerNumber() should return true for existing number")
    void existsByCustomerNumberShouldWork() {
        // Given
        Customer customer = createTestCustomer("exists@example.com", "EXISTS-001", "Test", "Exists");
        customerRepository.save(customer);

        // When/Then
        assertThat(customerRepository.existsByCustomerNumber(customer.getCustomerNumber())).isTrue();
        assertThat(customerRepository.existsByCustomerNumber("NON-EXISTENT")).isFalse();
    }

    @Test
    @Transactional
    @org.junit.jupiter.api.Disabled("Concurrency handling in QuarkusTest problematic with H2")
    @DisplayName("optimistic locking should prevent lost updates")
    void optimisticLockingTest() {
        // Given
        Customer customer = createTestCustomer("lock@example.com", "LOCK-001", "Lock", "Test");
        Customer saved = customerRepository.save(customer);

        Customer instance1 = customerRepository.findById(saved.getId()).get();
        Customer instance2 = customerRepository.findById(saved.getId()).get();

        // When
        instance1.updateEmail("update1@example.com");
        customerRepository.save(instance1);

        instance2.updateEmail("update2@example.com");

        // Then
        assertThrows(OptimisticLockException.class, () -> {
            customerRepository.save(instance2);
            customerRepository.findById(saved.getId());
        });
    }
}
