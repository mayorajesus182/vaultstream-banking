package com.vaultstream.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events in VaultStream.
 * 
 * All events are immutable and contain metadata about when and where they
 * occurred.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public abstract class DomainEvent {

    /**
     * Unique identifier for this event instance
     */
    private UUID eventId;

    /**
     * ID of the aggregate that produced this event
     */
    private UUID aggregateId;

    /**
     * Type of the aggregate (e.g., "Customer", "Account")
     */
    private String aggregateType;

    /**
     * Version of the aggregate after this event
     */
    private int version;

    /**
     * When the event occurred
     */
    private Instant occurredAt;

    /**
     * Optional correlation ID for distributed tracing
     */
    private String correlationId;

    /**
     * Optional causation ID (ID of the command that caused this event)
     */
    private String causationId;

    /**
     * Get the event type name (simple class name by default)
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    /**
     * Initialize event metadata
     */
    protected void initializeEventMetadata(UUID aggregateId, String aggregateType, int version) {
        this.eventId = UUID.randomUUID();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.version = version;
        this.occurredAt = Instant.now();
    }
}
