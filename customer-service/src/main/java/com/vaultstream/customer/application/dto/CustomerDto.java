package com.vaultstream.customer.application.dto;

import com.vaultstream.customer.domain.model.Customer;
import com.vaultstream.customer.domain.model.CustomerStatus;
import com.vaultstream.customer.domain.model.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer Data Transfer Object for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CustomerResponse", description = "Customer details response")
public class CustomerDto {

    @Schema(description = "Unique customer ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "Customer number/business key", example = "CUST-20260120-12345")
    private String customerNumber;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Full name", example = "John Doe")
    private String fullName;

    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Phone number", example = "+1234567890")
    private String phoneNumber;

    @Schema(description = "Date of birth", example = "1990-01-01")
    private LocalDate dateOfBirth;

    @Schema(description = "Address details")
    private AddressDto address;

    @Schema(description = "Customer status", example = "ACTIVE")
    private CustomerStatus status;

    @Schema(description = "Display name for status", example = "Active")
    private String statusDisplayName;

    @Schema(description = "Customer type", example = "INDIVIDUAL")
    private CustomerType type;

    @Schema(description = "Display name for type", example = "Individual")
    private String typeDisplayName;

    @Schema(description = "Creation timestamp", example = "2026-01-20T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-01-20T10:00:00")
    private LocalDateTime updatedAt;

    /**
     * Address DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "AddressResponse", description = "Address details")
    public static class AddressDto {
        @Schema(description = "Street name", example = "Main St")
        private String street;

        @Schema(description = "Street number", example = "123")
        private String number;

        @Schema(description = "Apartment/Unit", example = "Apt 4B")
        private String apartment;

        @Schema(description = "City", example = "New York")
        private String city;

        @Schema(description = "State/Province", example = "NY")
        private String state;

        @Schema(description = "Postal/Zip Code", example = "10001")
        private String postalCode;

        @Schema(description = "Country", example = "US")
        private String country;

        @Schema(description = "Full formatted address", example = "123 Main St, Apt 4B, New York, NY 10001, US")
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
