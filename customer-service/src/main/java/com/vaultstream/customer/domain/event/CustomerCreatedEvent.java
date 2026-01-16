package com.vaultstream.customer.domain.event;

import com.vaultstream.common.event.DomainEvent;
import com.vaultstream.common.event.IntegrationEvent;
import com.vaultstream.customer.domain.model.CustomerStatus;
import com.vaultstream.customer.domain.model.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Event emitted when a new customer is created.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreatedEvent extends DomainEvent implements IntegrationEvent {

    private String customerNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private CustomerType type;
    private CustomerStatus status;

    public static CustomerCreatedEvent fromCustomer(UUID customerId, String customerNumber,
            String firstName, String lastName, String email, String phoneNumber,
            LocalDate dateOfBirth, CustomerType type, CustomerStatus status) {

        CustomerCreatedEvent event = CustomerCreatedEvent.builder()
                .customerNumber(customerNumber)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phoneNumber(phoneNumber)
                .dateOfBirth(dateOfBirth)
                .type(type)
                .status(status)
                .build();

        event.initializeEventMetadata(customerId, "Customer", 1);
        return event;
    }

    @Override
    public String getTopic() {
        return "vaultstream.customer.created";
    }

    @Override
    public String getAggregateIdAsString() {
        return getAggregateId().toString();
    }
}
