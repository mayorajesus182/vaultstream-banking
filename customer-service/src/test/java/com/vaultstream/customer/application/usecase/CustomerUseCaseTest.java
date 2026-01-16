package com.vaultstream.customer.application.usecase;

import com.vaultstream.common.exception.BusinessRuleViolationException;
import com.vaultstream.common.exception.ResourceNotFoundException;
import com.vaultstream.customer.application.command.CreateCustomerCommand;
import com.vaultstream.customer.application.command.UpdateCustomerCommand;
import com.vaultstream.customer.application.dto.CustomerDto;
import com.vaultstream.customer.domain.model.Address;
import com.vaultstream.customer.domain.model.Customer;
import com.vaultstream.customer.domain.model.CustomerStatus;
import com.vaultstream.customer.domain.model.CustomerType;
import com.vaultstream.customer.domain.repository.CustomerRepository;
import com.vaultstream.customer.infrastructure.messaging.CustomerEventProducer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomerUseCase with mocked dependencies.
 */
@QuarkusTest
@DisplayName("CustomerUseCase")
class CustomerUseCaseTest {

    @Inject
    CustomerUseCase customerUseCase;

    @InjectMock
    CustomerRepository customerRepository;

    @InjectMock
    CustomerEventProducer eventProducer;

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
                "John", "Doe", "john.doe@test.com", "+1234567890",
                LocalDate.of(1990, 5, 15), "12345678A", null, CustomerType.INDIVIDUAL);
    }

    @Nested
    @DisplayName("createCustomer")
    class CreateCustomerTests {

        @BeforeEach
        void setUp() {
            reset(customerRepository, eventProducer);
        }

        @Test
        @DisplayName("should create customer successfully")
        void shouldCreateCustomerSuccessfully() {
            // Given
            CreateCustomerCommand command = createValidCommand();
            when(customerRepository.existsByEmail(anyString())).thenReturn(false);
            when(customerRepository.existsByNationalId(anyString())).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerDto result = customerUseCase.createCustomer(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getEmail()).isEqualTo("john.doe@test.com");
            assertThat(result.getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);

            verify(customerRepository).save(any(Customer.class));
            verify(eventProducer).publish(any());
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            CreateCustomerCommand command = createValidCommand();
            when(customerRepository.existsByEmail(anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> customerUseCase.createCustomer(command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("email");

            verify(customerRepository, never()).save(any());
            verify(eventProducer, never()).publish(any());
        }

        @Test
        @DisplayName("should throw exception when nationalId already exists")
        void shouldThrowExceptionWhenNationalIdExists() {
            // Given
            CreateCustomerCommand command = createValidCommand();
            when(customerRepository.existsByEmail(anyString())).thenReturn(false);
            when(customerRepository.existsByNationalId(anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> customerUseCase.createCustomer(command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("national ID");

            verify(customerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateCustomer")
    class UpdateCustomerTests {

        @BeforeEach
        void setUp() {
            reset(customerRepository, eventProducer);
        }

        @Test
        @DisplayName("should update customer successfully")
        void shouldUpdateCustomerSuccessfully() {
            // Given
            Customer existingCustomer = createTestCustomer();
            UUID customerId = existingCustomer.getId();

            UpdateCustomerCommand command = new UpdateCustomerCommand();
            command.setCustomerId(customerId.toString());
            command.setFirstName("Jane");
            command.setLastName("Smith");

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerDto result = customerUseCase.updateCustomer(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Smith");

            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("should throw exception when customer not found")
        void shouldThrowExceptionWhenCustomerNotFound() {
            // Given
            UUID customerId = UUID.randomUUID();
            UpdateCustomerCommand command = new UpdateCustomerCommand();
            command.setCustomerId(customerId.toString());

            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> customerUseCase.updateCustomer(command))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("activateCustomer")
    class ActivateCustomerTests {

        @BeforeEach
        void setUp() {
            reset(customerRepository, eventProducer);
        }

        @Test
        @DisplayName("should activate customer and publish event")
        void shouldActivateCustomerAndPublishEvent() {
            // Given
            Customer customer = createTestCustomer();
            UUID customerId = customer.getId();

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerDto result = customerUseCase.activateCustomer(customerId.toString());

            // Then
            assertThat(result.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            verify(eventProducer).publish(any());
        }
    }

    @Nested
    @DisplayName("suspendCustomer")
    class SuspendCustomerTests {

        @BeforeEach
        void setUp() {
            reset(customerRepository, eventProducer);
        }

        @Test
        @DisplayName("should suspend customer with reason")
        void shouldSuspendCustomerWithReason() {
            // Given
            Customer customer = createTestCustomer();
            customer.activate();
            UUID customerId = customer.getId();

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerDto result = customerUseCase.suspendCustomer(customerId.toString(), "Fraud investigation");

            // Then
            assertThat(result.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
            verify(eventProducer).publish(any());
        }
    }

    @Nested
    @DisplayName("Query Operations")
    class QueryTests {

        @BeforeEach
        void setUp() {
            reset(customerRepository, eventProducer);
        }

        @Test
        @DisplayName("getCustomerById should return customer DTO")
        void getCustomerByIdShouldReturnDto() {
            // Given
            Customer customer = createTestCustomer();
            UUID customerId = customer.getId();

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            // When
            CustomerDto result = customerUseCase.getCustomerById(customerId.toString());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(customerId.toString());
        }

        @Test
        @DisplayName("getCustomerById should throw when not found")
        void getCustomerByIdShouldThrowWhenNotFound() {
            // Given
            UUID customerId = UUID.randomUUID();
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> customerUseCase.getCustomerById(customerId.toString()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("getCustomersByStatus should return list of DTOs")
        void getCustomersByStatusShouldReturnList() {
            // Given
            Customer customer1 = createTestCustomer();
            customer1.activate();

            when(customerRepository.findByStatus(CustomerStatus.ACTIVE))
                    .thenReturn(List.of(customer1));

            // When
            var result = customerUseCase.getCustomersByStatus(CustomerStatus.ACTIVE);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        }
    }
}
