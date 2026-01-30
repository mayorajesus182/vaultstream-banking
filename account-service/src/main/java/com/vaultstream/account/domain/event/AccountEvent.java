package com.vaultstream.account.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events in the Account aggregate.
 * Events are immutable facts that happened in the past.
 */
public interface AccountEvent {

    /**
     * Unique identifier of this event
     */
    UUID getEventId();

    /**
     * The aggregate (account) this event belongs to
     */
    UUID getAccountId();

    /**
     * When this event occurred
     */
    Instant getOccurredAt();

    /**
     * Event type name for serialization
     */
    String getEventType();

    /**
     * Event version for schema evolution
     */
    default int getEventVersion() {
        return 1;
    }
}
