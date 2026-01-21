package com.vaultstream.customer.application.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to update an existing customer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateCustomerCommand", description = "Payload for updating an existing customer")
public class UpdateCustomerCommand {

    @NotBlank(message = "Customer ID is required")
    @Schema(hidden = true)
    private String customerId;

    @Schema(description = "Customer's first name", example = "Jane")
    private String firstName;

    @Schema(description = "Customer's last name", example = "Doe")
    private String lastName;

    @Email(message = "Invalid email format")
    @Schema(description = "Customer's email address", example = "jane.doe@example.com")
    private String email;

    @Schema(description = "Customer's phone number", example = "+1987654321")
    private String phoneNumber;

    @Past(message = "Date of birth must be in the past")
    @Schema(description = "Date of birth", example = "1992-05-15")
    private LocalDate dateOfBirth;

    @Valid
    @Schema(description = "Updated address details")
    private CreateCustomerCommand.AddressCommand address;

    public UUID getCustomerIdAsUUID() {
        return UUID.fromString(customerId);
    }
}
