package com.vaultstream.customer.application.usecase;

import com.vaultstream.common.event.IntegrationEvent;
import com.vaultstream.common.exception.BusinessRuleViolationException;
import com.vaultstream.customer.application.command.CreateCustomerCommand;
import com.vaultstream.customer.application.command.UpdateCustomerCommand;
import com.vaultstream.customer.application.dto.CustomerDto;
import com.vaultstream.customer.application.service.CustomerMetrics;
import com.vaultstream.customer.application.service.CustomerNumberGenerator;
import com.vaultstream.customer.domain.model.Customer;
import com.vaultstream.customer.domain.model.CustomerType;
import com.vaultstream.customer.domain.repository.CustomerRepository;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomerUseCase with mocked dependencies.
 */
@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@DisplayName("CustomerUseCase Unit Tests")
class CustomerUseCaseTest {

    @org.mockito.InjectMocks
    CustomerUseCase customerUseCase;

    @org.mockito.Mock
    CustomerRepository customerRepository;

    @org.mockito.Mock
    CustomerNumberGenerator customerNumberGenerator;

    @org.mockito.Mock
    CustomerMetrics customerMetrics;

    @org.mockito.Mock
    Event<IntegrationEvent> eventPublisher;

    @org.mockito.Mock
    CacheManager cacheManager;

    @BeforeEach
    void setup() {
        Cache cache = mock(Cache.class);
        lenient().when(cacheManager.getCache(anyString())).thenReturn(Optional.of(cache));
        lenient().when(customerNumberGenerator.generate()).thenReturn("CUST-TEST-001");
    }

    private CreateCustomerCommand createValidCommand() {
        CreateCustomerCommand command = new CreateCustomerCommand();
        command.setFirstName("John");
        command.setLastName("Doe");
        command.setEmail("john.doe@test.com");
        command.setPhoneNumber("+1234567890");
        command.setDateOfBirth(LocalDate.of(1990, 5, 15));
        command.setNationalId("12345678A");
        command.setType(CustomerType.INDIVIDUAL);
        return command;
    }

    private Customer createTestCustomer() {
        return Customer.create(
                "CUST-TEST-001",
                "John",
                "Doe",
                "john.doe@test.com",
                "+1234567890",
                LocalDate.of(1990, 5, 15),
                "12345678A",
                null,
                CustomerType.INDIVIDUAL);
    }

    private void setCustomerId(Customer customer, UUID id) {
        try {
            java.lang.reflect.Field field = Customer.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(customer, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID via reflection", e);
        }
    }

    @Test
    @DisplayName("createCustomer should succeed with valid data")
    void createCustomerSuccess() {
        CreateCustomerCommand command = createValidCommand();
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArguments()[0]);

        CustomerDto result = customerUseCase.createCustomer(command);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(command.getEmail());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("createCustomer should throw on duplicate email")
    void createCustomerDuplicateEmail() {
        CreateCustomerCommand command = createValidCommand();
        when(customerRepository.existsByEmail(command.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> customerUseCase.createCustomer(command))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("email already exists");
    }

    @Test
    @DisplayName("updateCustomer should invalidate cache")
    void updateCustomerShouldInvalidateCache() {
        UUID id = UUID.randomUUID();
        Customer customer = createTestCustomer();
        setCustomerId(customer, id);

        UpdateCustomerCommand command = new UpdateCustomerCommand();
        command.setCustomerId(id.toString());
        command.setEmail("new.email@test.com");

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        customerUseCase.updateCustomer(command);

        verify(cacheManager, atLeastOnce()).getCache("customer-cache");
    }

    @Test
    @DisplayName("activateCustomer should fire event")
    void activateCustomerShouldFireEvent() {
        UUID id = UUID.randomUUID();
        Customer customer = createTestCustomer();
        setCustomerId(customer, id);

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        customerUseCase.activateCustomer(id.toString());

        verify(eventPublisher).fire(any(IntegrationEvent.class));
        verify(customerMetrics).recordCustomerActivated();
    }

    @Test
    @DisplayName("getCustomerById should return customer")
    void getCustomerByIdSuccess() {
        UUID id = UUID.randomUUID();
        Customer customer = createTestCustomer();
        setCustomerId(customer, id);

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        CustomerDto result = customerUseCase.getCustomerById(id.toString());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id.toString());
    }
}
