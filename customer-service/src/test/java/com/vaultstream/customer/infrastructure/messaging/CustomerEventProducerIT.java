package com.vaultstream.customer.infrastructure.messaging;

import com.vaultstream.common.event.IntegrationEvent;
import com.vaultstream.customer.application.usecase.CustomerUseCase;
import com.vaultstream.customer.domain.model.Customer;
import com.vaultstream.customer.domain.model.CustomerType;
import com.vaultstream.customer.domain.repository.CustomerRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@DisplayName("Customer Event Producer Integration")
class CustomerEventProducerIT {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    CustomerUseCase customerUseCase;

    @Inject
    CustomerRepository customerRepository;

    @Test
    @DisplayName("should publish CustomerActivatedEvent to Kafka")
    void shouldPublishEventToKafka() {
        // Given
        InMemorySink<IntegrationEvent> events = connector.sink("customer-events-out");

        Customer customer = Customer.create(
                "CUST-EVENT-001", "Event", "Test", "event.test@example.com",
                "+1234567890", LocalDate.of(1990, 1, 1), "EVENT-ID-001", null, CustomerType.INDIVIDUAL);
        // Manually set ID to avoid issues if save is mocked (but here it is real H2)
        // Actually save to DB so activation works
        saveCustomer(customer);

        // When
        customerUseCase.activateCustomer(customer.getId().toString());

        // Then
        // Then
        await().until(() -> events.received().size() > 0);
        Message<String> message = events.received().get(0);
        String payload = message.getPayload();

        // Deserialize manually because producer sends String
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        IntegrationEvent event = objectMapper.readValue(payload, IntegrationEvent.class);

        assertThat(event.getAggregateIdAsString()).isEqualTo(customer.getId().toString());
        assertThat(event.getEventType()).isEqualTo("CustomerActivated");
    }

    @Transactional
    void saveCustomer(Customer customer) {
        customerRepository.save(customer);
    }
}
