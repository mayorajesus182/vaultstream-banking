package com.vaultstream.customer.infrastructure.persistence;

import com.vaultstream.customer.domain.model.Address;
import com.vaultstream.customer.domain.model.Customer;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper for Customer entity <-> domain model conversion.
 * 
 * Manual mapping is used because Customer has a private constructor
 * and uses factory methods.
 */
@ApplicationScoped
public class CustomerMapper {

    // ========================================
    // Entity -> Domain
    // ========================================

    public Customer toDomain(CustomerEntity entity) {
        if (entity == null) {
            return null;
        }

        Address address = toAddress(entity);

        // Use factory method to create Customer
        Customer customer = Customer.create(
                entity.getCustomerNumber(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getDateOfBirth(),
                entity.getNationalId(),
                address,
                entity.getType());

        // Use reflection to set package-private fields
        // This is necessary because setId and setVersion are package-private
        try {
            java.lang.reflect.Method setId = Customer.class.getDeclaredMethod("setId", java.util.UUID.class);
            setId.setAccessible(true);
            setId.invoke(customer, entity.getId());

            java.lang.reflect.Method setVersion = Customer.class.getDeclaredMethod("setVersion", int.class);
            setVersion.setAccessible(true);
            setVersion.invoke(customer, entity.getVersion());
        } catch (Exception e) {
            throw new RuntimeException("Failed to map CustomerEntity to Customer", e);
        }

        return customer;
    }

    private Address toAddress(CustomerEntity entity) {
        if (entity.getStreet() == null && entity.getCity() == null) {
            return null;
        }
        return Address.builder()
                .street(entity.getStreet())
                .number(entity.getStreetNumber())
                .apartment(entity.getApartment())
                .city(entity.getCity())
                .state(entity.getState())
                .postalCode(entity.getPostalCode())
                .country(entity.getCountry())
                .build();
    }

    // ========================================
    // Domain -> Entity
    // ========================================

    public CustomerEntity toEntity(Customer customer) {
        if (customer == null) {
            return null;
        }

        CustomerEntity.CustomerEntityBuilder builder = CustomerEntity.builder()
                .id(customer.getId())
                .customerNumber(customer.getCustomerNumber())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .dateOfBirth(customer.getDateOfBirth())
                .nationalId(customer.getNationalId())
                .status(customer.getStatus())
                .type(customer.getType())
                .suspensionReason(customer.getSuspensionReason())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .version(customer.getVersion());

        // Map address fields
        if (customer.getAddress() != null) {
            builder.street(customer.getAddress().getStreet())
                    .streetNumber(customer.getAddress().getNumber())
                    .apartment(customer.getAddress().getApartment())
                    .city(customer.getAddress().getCity())
                    .state(customer.getAddress().getState())
                    .postalCode(customer.getAddress().getPostalCode())
                    .country(customer.getAddress().getCountry());
        }

        return builder.build();
    }

    public void updateEntity(CustomerEntity entity, Customer customer) {
        if (entity == null || customer == null) {
            return;
        }

        entity.setCustomerNumber(customer.getCustomerNumber());
        entity.setFirstName(customer.getFirstName());
        entity.setLastName(customer.getLastName());
        entity.setEmail(customer.getEmail());
        entity.setPhoneNumber(customer.getPhoneNumber());
        entity.setDateOfBirth(customer.getDateOfBirth());
        entity.setNationalId(customer.getNationalId());
        entity.setStatus(customer.getStatus());
        entity.setType(customer.getType());
        entity.setSuspensionReason(customer.getSuspensionReason());
        entity.setUpdatedAt(customer.getUpdatedAt());
        entity.setVersion(customer.getVersion());

        // Map address fields
        if (customer.getAddress() != null) {
            entity.setStreet(customer.getAddress().getStreet());
            entity.setStreetNumber(customer.getAddress().getNumber());
            entity.setApartment(customer.getAddress().getApartment());
            entity.setCity(customer.getAddress().getCity());
            entity.setState(customer.getAddress().getState());
            entity.setPostalCode(customer.getAddress().getPostalCode());
            entity.setCountry(customer.getAddress().getCountry());
        } else {
            // Clear address fields if address is null
            entity.setStreet(null);
            entity.setStreetNumber(null);
            entity.setApartment(null);
            entity.setCity(null);
            entity.setState(null);
            entity.setPostalCode(null);
            entity.setCountry(null);
        }
    }
}
