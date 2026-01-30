package com.vaultstream.account.domain.event;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when money is deposited into an account.
 */
@Value
@Builder
public class MoneyDepositedEvent implements AccountEvent {

    @Builder.Default
    UUID eventId = UUID.randomUUID();

    UUID accountId;
    BigDecimal amount;
    BigDecimal balanceAfter;
    String description;
    String transactionReference;

    @Builder.Default
    Instant occurredAt = Instant.now();

    @Override
    public String getEventType() {
        return "MoneyDeposited";
    }
}
