package com.vaultstream.customer.application.dto;

import com.vaultstream.customer.domain.model.Customer;
import com.vaultstream.customer.domain.model.CustomerStatus;
import com.vaultstream.customer.domain.model.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer Data Transfer Object for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

    private String id;
    private String customerNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private AddressDto address;
    private CustomerStatus status;
    private String statusDisplayName;
    private CustomerType type;
    private String typeDisplayName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Address DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        private String street;
        private String number;
        private String apartment;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String fullAddress;
    }

    /**
     * Create DTO from domain entity
     */
    public static CustomerDto fromEntity(Customer customer) {
        if (customer == null) {
            return null;
        }

        CustomerDtoBuilder builder = CustomerDto.builder()
                .id(customer.getId().toString())
                .customerNumber(customer.getCustomerNumber())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .dateOfBirth(customer.getDateOfBirth())
                .status(customer.getStatus())
                .statusDisplayName(customer.getStatus().getDisplayName())
                .type(customer.getType())
                .typeDisplayName(customer.getType().getDisplayName())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt());

        if (customer.getAddress() != null) {
            builder.address(AddressDto.builder()
                    .street(customer.getAddress().getStreet())
                    .number(customer.getAddress().getNumber())
                    .apartment(customer.getAddress().getApartment())
                    .city(customer.getAddress().getCity())
                    .state(customer.getAddress().getState())
                    .postalCode(customer.getAddress().getPostalCode())
                    .country(customer.getAddress().getCountry())
                    .fullAddress(customer.getAddress().getFullAddress())
                    .build());
        }

        return builder.build();
    }
}
