package com.vaultstream.account.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to withdraw money from an account.
 */
@Value
@Builder
public class WithdrawMoneyCommand {

    @NotNull(message = "Account ID is required")
    UUID accountId;

    @Positive(message = "Amount must be positive")
    BigDecimal amount;

    @NotBlank(message = "Description is required")
    String description;

    String transactionReference;
}
