package com.vaultstream.account.infrastructure.messaging;

import com.vaultstream.account.application.command.CreateAccountCommand;
import com.vaultstream.account.application.service.AccountCommandHandler;
import com.vaultstream.account.domain.model.AccountType;
import com.vaultstream.common.event.IntegrationEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for customer events.
 * 
 * Automatically creates a savings account when a customer is activated.
 */
@Slf4j
@ApplicationScoped
public class CustomerEventConsumer {

    private static final String DEFAULT_CURRENCY = "USD";

    @Inject
    AccountCommandHandler commandHandler;

    /**
     * Handle customer events from Kafka
     */
    @Incoming("customer-events-in")
    @Blocking
    public void onCustomerEvent(IntegrationEvent event) {
        log.info("Received customer event: {} - {}", event.getEventType(), event.getAggregateIdAsString());

        try {
            switch (event.getEventType()) {
                case "CustomerActivated" -> handleCustomerActivated(event);
                case "CustomerDeactivated" -> handleCustomerDeactivated(event);
                default -> log.debug("Ignoring event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing customer event: {}", event, e);
            // In production, implement proper error handling (DLQ, retry, etc.)
        }
    }

    /**
     * Create a default savings account when customer is activated
     */
    private void handleCustomerActivated(IntegrationEvent event) {
        UUID customerId = UUID.fromString(event.getAggregateIdAsString());
        
        // Check if customer already has accounts (idempotency)
        // In production, you'd query the read model here

        log.info("Creating default savings account for customer: {}", customerId);

        CreateAccountCommand command = CreateAccountCommand.builder()
                .customerId(customerId)
                .accountType(AccountType.SAVINGS)
                .currency(DEFAULT_CURRENCY)
                .initialDeposit(BigDecimal.ZERO)
                .build();

        try {
            var account = commandHandler.createAccount(command);
            log.info("Created account {} for customer {}", account.getAccountNumber(), customerId);
        } catch (Exception e) {
            log.error("Failed to create account for customer: {}", customerId, e);
        }
    }

    /**
     * Handle customer deactivation - could freeze related accounts
     */
    private void handleCustomerDeactivated(IntegrationEvent event) {
        UUID customerId = UUID.fromString(event.getAggregateIdAsString());
        log.info("Customer deactivated: {}. Consider freezing accounts.", customerId);
        // In production, implement account freeze logic here
    }
}
