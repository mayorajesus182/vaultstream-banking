package com.vaultstream.customer.infrastructure.persistence;

import com.vaultstream.customer.domain.model.Address;
import com.vaultstream.customer.domain.model.Customer;
import com.vaultstream.customer.domain.model.CustomerStatus;
import com.vaultstream.customer.domain.model.CustomerType;
import com.vaultstream.customer.domain.repository.CustomerRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CustomerRepository with H2 database.
 */
@QuarkusTest
@DisplayName("CustomerRepository Integration")
class CustomerRepositoryIT {

    @Inject
    CustomerRepository customerRepository;

    private Customer createTestCustomer(String email, String nationalId) {
        Address address = Address.builder()
                .street("Test Street")
                .number("123")
                .city("Test City")
                .state("TS")
                .postalCode("12345")
                .country("USA")
                .build();

        return Customer.create(
                "Test",
                "User",
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
        Customer customer = createTestCustomer("save.test@example.com", "SAVE-001");

        // When
        Customer saved = customerRepository.save(customer);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCustomerNumber()).startsWith("CUST-");
    }

    @Test
    @Transactional
    @DisplayName("findById() should return customer when exists")
    void findByIdShouldReturnCustomer() {
        // Given
        Customer customer = createTestCustomer("findbyid.test@example.com", "FIND-001");
        Customer saved = customerRepository.save(customer);

        // When
        Optional<Customer> found = customerRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("findbyid.test@example.com");
    }

    @Test
    @Transactional
    @DisplayName("findById() should return empty when not exists")
    void findByIdShouldReturnEmptyWhenNotExists() {
        // When
        Optional<Customer> found = customerRepository.findById(UUID.randomUUID());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("existsByEmail() should return true when email exists")
    void existsByEmailShouldReturnTrue() {
        // Given
        Customer customer = createTestCustomer("exists.email@example.com", "EXISTS-001");
        customerRepository.save(customer);

        // When
        boolean exists = customerRepository.existsByEmail("EXISTS.EMAIL@example.com"); // Different case

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("existsByNationalId() should return true when nationalId exists")
    void existsByNationalIdShouldReturnTrue() {
        // Given
        Customer customer = createTestCustomer("national.id@example.com", "NATIONAL-001");
        customerRepository.save(customer);

        // When
        boolean exists = customerRepository.existsByNationalId("NATIONAL-001");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("findByStatus() should return customers with given status")
    void findByStatusShouldReturnMatchingCustomers() {
        // Given
        Customer customer1 = createTestCustomer("status1@example.com", "STATUS-001");
        customer1.activate();
        customerRepository.save(customer1);

        Customer customer2 = createTestCustomer("status2@example.com", "STATUS-002");
        customer2.activate();
        customerRepository.save(customer2);

        // When
        List<Customer> activeCustomers = customerRepository.findByStatus(CustomerStatus.ACTIVE);

        // Then
        assertThat(activeCustomers).hasSizeGreaterThanOrEqualTo(2);
        assertThat(activeCustomers).allMatch(c -> c.getStatus() == CustomerStatus.ACTIVE);
    }

    @Test
    @Transactional
    @DisplayName("searchByName() should find customers by partial name")
    void searchByNameShouldFindByPartialName() {
        // Given
        Customer customer = createTestCustomer("search.name@example.com", "SEARCH-001");
        customerRepository.save(customer);

        // When - search by "Test" which is the firstName
        List<Customer> results = customerRepository.searchByName("test", 0, 10);

        // Then
        assertThat(results).isNotEmpty();
    }

    @Test
    @Transactional
    @DisplayName("save() should update existing customer")
    void saveShouldUpdateExistingCustomer() {
        // Given
        Customer customer = createTestCustomer("update.repo@example.com", "UPDATE-001");
        Customer saved = customerRepository.save(customer);
        UUID customerId = saved.getId();

        // When
        saved.updatePersonalInfo("Updated", "Name", null, null);
        Customer updated = customerRepository.save(saved);

        // Then
        assertThat(updated.getId()).isEqualTo(customerId);
        assertThat(updated.getFirstName()).isEqualTo("Updated");
        assertThat(updated.getLastName()).isEqualTo("Name");
    }

    @Test
    @Transactional
    @DisplayName("count() should return total number of customers")
    void countShouldReturnTotal() {
        // Given
        long initialCount = customerRepository.count();
        customerRepository.save(createTestCustomer("count1@example.com", "COUNT-001"));
        customerRepository.save(createTestCustomer("count2@example.com", "COUNT-002"));

        // When
        long newCount = customerRepository.count();

        // Then
        assertThat(newCount).isEqualTo(initialCount + 2);
    }
}
