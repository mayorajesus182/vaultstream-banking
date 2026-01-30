package com.vaultstream.account.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultstream.account.domain.event.*;
import com.vaultstream.account.domain.model.Account;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Event Store implementation using PostgreSQL.
 * 
 * Persists domain events and reconstructs aggregates from event history.
 */
@Slf4j
@ApplicationScoped
public class EventStore {

    private static final String AGGREGATE_TYPE = "Account";

    @Inject
    EntityManager em;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Save all uncommitted events from an aggregate
     */
    @Transactional
    public void saveEvents(Account account) {
        List<AccountEvent> events = account.getUncommittedEvents();
        long currentVersion = getCurrentVersion(account.getId());

        for (AccountEvent event : events) {
            currentVersion++;
            EventStoreEntity entity = toEntity(event, currentVersion);
            em.persist(entity);
            log.debug("Persisted event: {} for account: {}", event.getEventType(), account.getId());
        }

        account.markEventsAsCommitted();
        log.info("Saved {} events for account: {}", events.size(), account.getId());
    }

    /**
     * Load an account aggregate from its event history
     */
    public Optional<Account> loadAggregate(UUID accountId) {
        List<AccountEvent> events = loadEvents(accountId);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Account.fromHistory(events));
    }

    /**
     * Load all events for an aggregate
     */
    public List<AccountEvent> loadEvents(UUID aggregateId) {
        List<EventStoreEntity> entities = em.createQuery(
                "SELECT e FROM EventStoreEntity e WHERE e.aggregateId = :id ORDER BY e.version",
                EventStoreEntity.class)
                .setParameter("id", aggregateId)
                .getResultList();

        return entities.stream()
                .map(this::fromEntity)
                .toList();
    }

    /**
     * Get current version of an aggregate
     */
    public long getCurrentVersion(UUID aggregateId) {
        Long version = em.createQuery(
                "SELECT MAX(e.version) FROM EventStoreEntity e WHERE e.aggregateId = :id",
                Long.class)
                .setParameter("id", aggregateId)
                .getSingleResult();
        return version != null ? version : 0L;
    }

    private EventStoreEntity toEntity(AccountEvent event, long version) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            return EventStoreEntity.builder()
                    .eventId(event.getEventId())
                    .aggregateId(event.getAccountId())
                    .aggregateType(AGGREGATE_TYPE)
                    .eventType(event.getEventType())
                    .eventVersion(event.getEventVersion())
                    .version(version)
                    .payload(payload)
                    .occurredAt(event.getOccurredAt())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    private AccountEvent fromEntity(EventStoreEntity entity) {
        try {
            Class<? extends AccountEvent> eventClass = getEventClass(entity.getEventType());
            return objectMapper.readValue(entity.getPayload(), eventClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }

    private Class<? extends AccountEvent> getEventClass(String eventType) {
        return switch (eventType) {
            case "AccountCreated" -> AccountCreatedEvent.class;
            case "MoneyDeposited" -> MoneyDepositedEvent.class;
            case "MoneyWithdrawn" -> MoneyWithdrawnEvent.class;
            case "AccountStatusChanged" -> AccountStatusChangedEvent.class;
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}
