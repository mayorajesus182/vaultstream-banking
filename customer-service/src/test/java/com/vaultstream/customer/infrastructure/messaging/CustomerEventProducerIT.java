package com.vaultstream.customer.infrastructure.messaging;

import com.vaultstream.customer.domain.event.CustomerStatusChangedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultstream.customer.application.usecase.CustomerUseCase;
import com.vaultstream.customer.domain.model.Customer;
import com.vaultstream.customer.domain.model.CustomerType;
import com.vaultstream.customer.domain.repository.CustomerRepository;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(CustomerEventProducerIT.NoCacheProfile.class)
@DisplayName("Customer Event Producer Integration")
class CustomerEventProducerIT {

    /**
     * Test profile that disables caching to avoid Redis connection errors in tests
     */
    public static class NoCacheProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> config = new HashMap<>();
            // Disable all caching for this test
            config.put("quarkus.cache.enabled", "false");
            return config;
        }
    }

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    CustomerUseCase customerUseCase;

    @Inject
    CustomerRepository customerRepository;

    @InjectMock
    RedisDataSource redisDataSource;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        // Mock Redis ValueCommands for Rate Limiting and Caching
        ValueCommands<String, Long> valueCommands = mock(ValueCommands.class);
        KeyCommands<String> keyCommands = mock(KeyCommands.class);

        when(redisDataSource.value(Long.class)).thenReturn(valueCommands);
        when(redisDataSource.key()).thenReturn(keyCommands);

        // Always allow rate limit (return 1 for first request)
        when(valueCommands.incr(anyString())).thenReturn(1L);
        when(valueCommands.get(anyString())).thenReturn(1L);
        when(keyCommands.expire(anyString(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("should publish CustomerActivatedEvent to Kafka")
    void shouldPublishEventToKafka() throws Exception {
        // Given
        InMemorySink<String> events = connector.sink("customer-events-out");

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

        // Deserialize to concrete event type (not interface) to avoid Jackson
        // deserialization error
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        CustomerStatusChangedEvent event = objectMapper.readValue(payload, CustomerStatusChangedEvent.class);

        assertThat(event.getAggregateIdAsString()).isEqualTo(customer.getId().toString());
        assertThat(event.getEventType()).isEqualTo("CustomerStatusChangedEvent");
    }

    @Transactional
    void saveCustomer(Customer customer) {
        customerRepository.save(customer);
    }
}
