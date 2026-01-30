package com.vaultstream.account.domain.event;

import com.vaultstream.account.domain.model.AccountStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when an account status changes.
 */
@Value
@Builder
public class AccountStatusChangedEvent implements AccountEvent {

    @Builder.Default
    UUID eventId = UUID.randomUUID();

    UUID accountId;
    AccountStatus previousStatus;
    AccountStatus newStatus;
    String reason;

    @Builder.Default
    Instant occurredAt = Instant.now();

    @Override
    public String getEventType() {
        return "AccountStatusChanged";
    }
}
