package com.vaultstream.account.application.command;

import com.vaultstream.account.domain.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to create a new account.
 */
@Value
@Builder
public class CreateAccountCommand {

    @NotNull(message = "Customer ID is required")
    UUID customerId;

    @NotNull(message = "Account type is required")
    AccountType accountType;

    @NotBlank(message = "Currency is required")
    String currency;

    @PositiveOrZero(message = "Initial deposit must be zero or positive")
    @Builder.Default
    BigDecimal initialDeposit = BigDecimal.ZERO;
}
