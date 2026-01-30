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

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheResult;

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

        private final CustomerRepository customerRepository;
        private final Event<com.vaultstream.common.event.IntegrationEvent> eventPublisher;
        private final com.vaultstream.customer.application.service.CustomerNumberGenerator customerNumberGenerator;
        private final com.vaultstream.customer.application.service.CustomerMetrics customerMetrics;
        private final CacheManager cacheManager;

        @Inject
        public CustomerUseCase(
                        CustomerRepository customerRepository,
                        Event<com.vaultstream.common.event.IntegrationEvent> eventPublisher,
                        com.vaultstream.customer.application.service.CustomerNumberGenerator customerNumberGenerator,
                        com.vaultstream.customer.application.service.CustomerMetrics customerMetrics,
                        CacheManager cacheManager) {
                this.customerRepository = customerRepository;
                this.eventPublisher = eventPublisher;
                this.customerNumberGenerator = customerNumberGenerator;
                this.customerMetrics = customerMetrics;
                this.cacheManager = cacheManager;
        }

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

                // Map address using helper method
                Address address = mapAddress(command.getAddress());

                // Generate unique customer number
                String customerNumber = customerNumberGenerator.generate();

                // Create domain entity
                Customer customer = Customer.create(
                                customerNumber,
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

                // Record metrics
                customerMetrics.recordCustomerCreated();

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

                // Update address if provided using helper method
                if (command.getAddress() != null) {
                        Address address = mapAddress(command.getAddress());
                        customer.updateAddress(address);
                }

                Customer saved = customerRepository.save(customer);
                log.info("Customer updated: {}", saved.getId());

                // Record metrics
                customerMetrics.recordCustomerUpdated();

                // Manually invalidate cache
                cacheManager.getCache("customer-cache")
                                .ifPresent(cache -> cache.invalidate(command.getCustomerId()));

                return CustomerDto.fromEntity(saved);
        }

        /**
         * Activate a customer
         */
        @Transactional
        @CacheInvalidate(cacheName = "customer-cache")
        public CustomerDto activateCustomer(@CacheKey String customerId) {
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

                // Record metrics
                customerMetrics.recordCustomerActivated();

                return CustomerDto.fromEntity(saved);
        }

        /**
         * Suspend a customer
         */
        @Transactional
        @CacheInvalidate(cacheName = "customer-cache")
        public CustomerDto suspendCustomer(@CacheKey String customerId, String reason) {
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

                // Record metrics
                customerMetrics.recordCustomerSuspended();

                return CustomerDto.fromEntity(saved);
        }

        /**
         * Deactivate a customer
         */
        @Transactional
        @CacheInvalidate(cacheName = "customer-cache")
        public void deactivateCustomer(@CacheKey String customerId) {
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

                // Record metrics
                customerMetrics.recordCustomerDeactivated();
        }

        // ========================================
        // Queries
        // ========================================

        /**
         * Get customer by ID
         */
        @CacheResult(cacheName = "customer-cache")
        public CustomerDto getCustomerById(@CacheKey String customerId) {
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
                long total = customerRepository.countByNameSearch(name);

                List<CustomerDto> dtos = customers.stream()
                                .map(CustomerDto::fromEntity)
                                .toList();

                return PageResponse.of(dtos, page, size, total);
        }

        /**
         * Get customers by status
         */
        public List<CustomerDto> getCustomersByStatus(CustomerStatus status) {
                return customerRepository.findByStatus(status).stream()
                                .map(CustomerDto::fromEntity)
                                .toList();
        }

        // ========================================
        // Private Helper Methods
        // ========================================

        /**
         * Maps AddressCommand to Address domain object
         */
        private Address mapAddress(CreateCustomerCommand.AddressCommand addressCommand) {
                if (addressCommand == null) {
                        return null;
                }
                return Address.builder()
                                .street(addressCommand.getStreet())
                                .number(addressCommand.getNumber())
                                .apartment(addressCommand.getApartment())
                                .city(addressCommand.getCity())
                                .state(addressCommand.getState())
                                .postalCode(addressCommand.getPostalCode())
                                .country(addressCommand.getCountry())
                                .build();
        }
}
