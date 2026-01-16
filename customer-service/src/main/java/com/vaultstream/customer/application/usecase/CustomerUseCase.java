package com.vaultstream.customer.application.usecase;

import com.vaultstream.common.dto.PageResponse;
import com.vaultstream.common.exception.BusinessRuleViolationException;
import com.vaultstream.common.exception.ResourceNotFoundException;
import com.vaultstream.customer.application.command.CreateCustomerCommand;
import com.vaultstream.customer.application.command.UpdateCustomerCommand;
import com.vaultstream.customer.application.dto.CustomerDto;
import com.vaultstream.customer.domain.event.CustomerCreatedEvent;
import com.vaultstream.customer.domain.event.CustomerStatusChangedEvent;
import com.vaultstream.customer.domain.model.Address;
import com.vaultstream.customer.domain.model.Customer;
import com.vaultstream.customer.domain.model.CustomerStatus;
import com.vaultstream.customer.domain.repository.CustomerRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * Customer Use Case - Application Service.
 * 
 * Orchestrates customer operations, handles transactions,
 * and publishes domain events.
 */
@Slf4j
@ApplicationScoped
public class CustomerUseCase {

        @Inject
        CustomerRepository customerRepository;

        @Inject
        Event<com.vaultstream.common.event.IntegrationEvent> eventPublisher;

        // ========================================
        // Commands
        // ========================================

        /**
         * Create a new customer
         */
        @Transactional
        public CustomerDto createCustomer(CreateCustomerCommand command) {
                log.info("Creating customer with email: {}", command.getEmail());

                // Validate uniqueness
                if (customerRepository.existsByEmail(command.getEmail())) {
                        throw new BusinessRuleViolationException("DUPLICATE_EMAIL",
                                        "A customer with this email already exists: " + command.getEmail());
                }

                if (customerRepository.existsByNationalId(command.getNationalId())) {
                        throw new BusinessRuleViolationException("DUPLICATE_NATIONAL_ID",
                                        "A customer with this national ID already exists");
                }

                // Map address
                Address address = null;
                if (command.getAddress() != null) {
                        address = Address.builder()
                                        .street(command.getAddress().getStreet())
                                        .number(command.getAddress().getNumber())
                                        .apartment(command.getAddress().getApartment())
                                        .city(command.getAddress().getCity())
                                        .state(command.getAddress().getState())
                                        .postalCode(command.getAddress().getPostalCode())
                                        .country(command.getAddress().getCountry())
                                        .build();
                }

                // Create domain entity
                Customer customer = Customer.create(
                                command.getFirstName(),
                                command.getLastName(),
                                command.getEmail(),
                                command.getPhoneNumber(),
                                command.getDateOfBirth(),
                                command.getNationalId(),
                                address,
                                command.getType());

                // Persist
                Customer saved = customerRepository.save(customer);
                log.info("Customer created with ID: {} and number: {}", saved.getId(), saved.getCustomerNumber());

                // Publish event
                CustomerCreatedEvent event = CustomerCreatedEvent.fromCustomer(
                                saved.getId(),
                                saved.getCustomerNumber(),
                                saved.getFirstName(),
                                saved.getLastName(),
                                saved.getEmail(),
                                saved.getPhoneNumber(),
                                saved.getDateOfBirth(),
                                saved.getType(),
                                saved.getStatus());

                eventPublisher.fire(event);

                return CustomerDto.fromEntity(saved);
        }

        /**
         * Update customer information
         */
        @Transactional
        public CustomerDto updateCustomer(UpdateCustomerCommand command) {
                log.info("Updating customer: {}", command.getCustomerId());

                Customer customer = customerRepository.findById(command.getCustomerIdAsUUID())
                                .orElseThrow(() -> new ResourceNotFoundException("Customer", command.getCustomerId()));

                // Update personal info
                customer.updatePersonalInfo(
                                command.getFirstName(),
                                command.getLastName(),
                                command.getPhoneNumber(),
                                command.getDateOfBirth());

                // Update email if provided
                if (command.getEmail() != null && !command.getEmail().equals(customer.getEmail())) {
                        if (customerRepository.existsByEmail(command.getEmail())) {
                                throw new BusinessRuleViolationException("DUPLICATE_EMAIL",
                                                "A customer with this email already exists");
                        }
                        customer.updateEmail(command.getEmail());
                }

                // Update address if provided
                if (command.getAddress() != null) {
                        Address address = Address.builder()
                                        .street(command.getAddress().getStreet())
                                        .number(command.getAddress().getNumber())
                                        .apartment(command.getAddress().getApartment())
                                        .city(command.getAddress().getCity())
                                        .state(command.getAddress().getState())
                                        .postalCode(command.getAddress().getPostalCode())
                                        .country(command.getAddress().getCountry())
                                        .build();
                        customer.updateAddress(address);
                }

                Customer saved = customerRepository.save(customer);
                log.info("Customer updated: {}", saved.getId());

                return CustomerDto.fromEntity(saved);
        }

        /**
         * Activate a customer
         */
        @Transactional
        public CustomerDto activateCustomer(String customerId) {
                log.info("Activating customer: {}", customerId);

                Customer customer = customerRepository.findById(UUID.fromString(customerId))
                                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

                CustomerStatus previousStatus = customer.getStatus();
                customer.activate();

                Customer saved = customerRepository.save(customer);

                // Publish status change event
                CustomerStatusChangedEvent event = CustomerStatusChangedEvent.create(
                                saved.getId(),
                                saved.getCustomerNumber(),
                                previousStatus,
                                saved.getStatus(),
                                "Customer activated after verification",
                                saved.getVersion());

                eventPublisher.fire(event);

                return CustomerDto.fromEntity(saved);
        }

        /**
         * Suspend a customer
         */
        @Transactional
        public CustomerDto suspendCustomer(String customerId, String reason) {
                log.info("Suspending customer: {} - Reason: {}", customerId, reason);

                Customer customer = customerRepository.findById(UUID.fromString(customerId))
                                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

                CustomerStatus previousStatus = customer.getStatus();
                customer.suspend(reason);

                Customer saved = customerRepository.save(customer);

                // Publish status change event
                CustomerStatusChangedEvent event = CustomerStatusChangedEvent.create(
                                saved.getId(),
                                saved.getCustomerNumber(),
                                previousStatus,
                                saved.getStatus(),
                                reason,
                                saved.getVersion());

                eventPublisher.fire(event);

                return CustomerDto.fromEntity(saved);
        }

        /**
         * Deactivate a customer
         */
        @Transactional
        public void deactivateCustomer(String customerId) {
                log.info("Deactivating customer: {}", customerId);

                Customer customer = customerRepository.findById(UUID.fromString(customerId))
                                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

                CustomerStatus previousStatus = customer.getStatus();
                customer.deactivate();

                Customer saved = customerRepository.save(customer);

                // Publish status change event
                CustomerStatusChangedEvent event = CustomerStatusChangedEvent.create(
                                saved.getId(),
                                saved.getCustomerNumber(),
                                previousStatus,
                                saved.getStatus(),
                                "Customer deactivated",
                                saved.getVersion());

                eventPublisher.fire(event);
        }

        // ========================================
        // Queries
        // ========================================

        /**
         * Get customer by ID
         */
        public CustomerDto getCustomerById(String customerId) {
                Customer customer = customerRepository.findById(UUID.fromString(customerId))
                                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
                return CustomerDto.fromEntity(customer);
        }

        /**
         * Get customer by customer number
         */
        public CustomerDto getCustomerByNumber(String customerNumber) {
                Customer customer = customerRepository.findByCustomerNumber(customerNumber)
                                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerNumber));
                return CustomerDto.fromEntity(customer);
        }

        /**
         * Get all customers with pagination
         */
        public PageResponse<CustomerDto> getAllCustomers(int page, int size) {
                List<Customer> customers = customerRepository.findAll(page, size);
                long total = customerRepository.count();

                List<CustomerDto> dtos = customers.stream()
                                .map(CustomerDto::fromEntity)
                                .toList();

                return PageResponse.of(dtos, page, size, total);
        }

        /**
         * Search customers by name
         */
        public PageResponse<CustomerDto> searchByName(String name, int page, int size) {
                List<Customer> customers = customerRepository.searchByName(name, page, size);

                List<CustomerDto> dtos = customers.stream()
                                .map(CustomerDto::fromEntity)
                                .toList();

                // For search, we return what we found (could be improved with separate count
                // query)
                return PageResponse.of(dtos, page, size, dtos.size());
        }

        /**
         * Get customers by status
         */
        public List<CustomerDto> getCustomersByStatus(CustomerStatus status) {
                return customerRepository.findByStatus(status).stream()
                                .map(CustomerDto::fromEntity)
                                .toList();
        }
}
