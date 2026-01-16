package com.vaultstream.customer.application.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to update an existing customer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerCommand {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    private String firstName;
    private String lastName;

    @Email(message = "Invalid email format")
    private String email;

    private String phoneNumber;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Valid
    private CreateCustomerCommand.AddressCommand address;

    public UUID getCustomerIdAsUUID() {
        return UUID.fromString(customerId);
    }
}
