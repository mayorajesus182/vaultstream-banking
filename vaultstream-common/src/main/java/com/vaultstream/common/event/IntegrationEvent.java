package com.vaultstream.common.event;

/**
 * Marker interface for integration events.
 * 
 * Integration events are published externally (e.g., to Kafka)
 * and can cross service boundaries.
 */
public interface IntegrationEvent {

    /**
     * Get the topic name for this event
     */
    default String getTopic() {
        return "vaultstream." + getEventType().toLowerCase().replace("event", "");
    }

    /**
     * Get the event type
     */
    String getEventType();

    /**
     * Get the aggregate ID as string (used as Kafka key)
     */
    String getAggregateIdAsString();
}
