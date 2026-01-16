package com.vaultstream.customer.domain.event;

import com.vaultstream.common.event.DomainEvent;
import com.vaultstream.common.event.IntegrationEvent;
import com.vaultstream.customer.domain.model.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Event emitted when customer status changes.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatusChangedEvent extends DomainEvent implements IntegrationEvent {

    private String customerNumber;
    private CustomerStatus previousStatus;
    private CustomerStatus newStatus;
    private String reason;

    public static CustomerStatusChangedEvent create(UUID customerId, String customerNumber,
            CustomerStatus previousStatus, CustomerStatus newStatus, String reason, int version) {

        CustomerStatusChangedEvent event = CustomerStatusChangedEvent.builder()
                .customerNumber(customerNumber)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(reason)
                .build();

        event.initializeEventMetadata(customerId, "Customer", version);
        return event;
    }

    @Override
    public String getTopic() {
        return "vaultstream.customer.status-changed";
    }

    @Override
    public String getAggregateIdAsString() {
        return getAggregateId().toString();
    }
}
