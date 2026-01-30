package com.vaultstream.account.infrastructure.persistence;

import com.vaultstream.account.domain.event.AccountEvent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a stored event in the Event Store.
 */
@Entity
@Table(name = "account_events", indexes = {
    @Index(name = "idx_account_events_aggregate", columnList = "aggregateId, version"),
    @Index(name = "idx_account_events_timestamp", columnList = "occurredAt")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStoreEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
