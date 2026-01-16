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

/**
 * Command to create a new customer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerCommand {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phoneNumber;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "National ID is required")
    private String nationalId;

    @Valid
    private AddressCommand address;

    private CustomerType type;

    /**
     * Nested address command
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressCommand {

        @NotBlank(message = "Street is required")
        private String street;

        private String number;
        private String apartment;

        @NotBlank(message = "City is required")
        private String city;

        private String state;
        private String postalCode;

        @NotBlank(message = "Country is required")
        private String country;
    }
}
