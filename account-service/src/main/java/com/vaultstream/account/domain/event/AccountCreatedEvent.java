package com.vaultstream.account.domain.event;

import com.vaultstream.account.domain.model.AccountStatus;
import com.vaultstream.account.domain.model.AccountType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

/**
 * Event raised when a new account is created.
 */
@Value
@Builder
public class AccountCreatedEvent implements AccountEvent {

    @Builder.Default
    UUID eventId = UUID.randomUUID();

    UUID accountId;
    String accountNumber;
    UUID customerId;
    AccountType accountType;
    AccountStatus status;
    BigDecimal initialBalance;
    Currency currency;

    @Builder.Default
    Instant occurredAt = Instant.now();

    @Override
    public String getEventType() {
        return "AccountCreated";
    }
}
