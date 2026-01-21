package com.vaultstream.customer.application.command;

import com.vaultstream.customer.domain.model.CustomerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Command to create a new customer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateCustomerCommand", description = "Payload for creating a new customer")
public class CreateCustomerCommand {

    @NotBlank(message = "First name is required")
    @Schema(description = "Customer's first name", example = "John", required = true)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "Customer's last name", example = "Doe", required = true)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Customer's email address", example = "john.doe@example.com", required = true)
    private String email;

    @Schema(description = "Customer's phone number in E.164 format", example = "+1234567890")
    private String phoneNumber;

    @Past(message = "Date of birth must be in the past")
    @Schema(description = "Date of birth", example = "1990-01-01")
    private LocalDate dateOfBirth;

    @NotBlank(message = "National ID is required")
    @Schema(description = "National ID or Passport number", example = "AB123456", required = true)
    private String nationalId;

    @Valid
    @Schema(description = "Customer's address")
    private AddressCommand address;

    @Schema(description = "Customer type", example = "INDIVIDUAL", defaultValue = "INDIVIDUAL")
    private CustomerType type;

    /**
     * Nested address command
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "AddressCommand", description = "Address details")
    public static class AddressCommand {

        @NotBlank(message = "Street is required")
        @Schema(description = "Street name", example = "Main St", required = true)
        private String street;

        @Schema(description = "Street number", example = "123")
        private String number;

        @Schema(description = "Apartment or suite number", example = "Apt 4B")
        private String apartment;

        @NotBlank(message = "City is required")
        @Schema(description = "City", example = "New York", required = true)
        private String city;

        @Schema(description = "State or province code", example = "NY")
        private String state;

        @Schema(description = "Postal or ZIP code", example = "10001")
        private String postalCode;

        @NotBlank(message = "Country is required")
        @Schema(description = "Country code (ISO 3166-1 alpha-2 or full name)", example = "US", required = true)
        private String country;
    }
}
