package com.vaultstream.customer.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultstream.common.event.IntegrationEvent;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.nio.charset.StandardCharsets;

/**
 * Kafka producer for customer domain events.
 */
@Slf4j
@ApplicationScoped
public class CustomerEventProducer {

    @Inject
    @Channel("customer-events-out")
    Emitter<String> emitter;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Publish an integration event to Kafka
     */
    public void publish(IntegrationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = event.getAggregateIdAsString();

            // Add headers for event type
            RecordHeaders headers = new RecordHeaders();
            headers.add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));
            headers.add("topic", event.getTopic().getBytes(StandardCharsets.UTF_8));

            OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                    .withKey(key)
                    .withHeaders(headers)
                    .build();

            Message<String> message = Message.of(payload).addMetadata(metadata);

            emitter.send(message);

            log.debug("📤 Event published: {} -> Key: {}", event.getEventType(), key);

        } catch (Exception e) {
            log.error("❌ Failed to publish event: {}", event.getEventType(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Observer method that listens for IntegrationEvent and publishes to Kafka
     * ONLY after the transaction has completed successfully.
     * This prevents "Enlisted connection used without active transaction" errors.
     */
    public void onDomainEvent(
            @jakarta.enterprise.event.Observes(during = jakarta.enterprise.event.TransactionPhase.AFTER_SUCCESS) IntegrationEvent event) {
        log.debug("🔄 Handling domain event after transaction success: {}", event.getEventType());
        publish(event);
    }
}
