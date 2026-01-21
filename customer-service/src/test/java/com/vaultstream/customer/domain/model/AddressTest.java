package com.vaultstream.customer.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Address Value Object")
class AddressTest {

    @Test
    @DisplayName("should create valid address and format full address")
    void shouldCreateValidAddress() {
        Address address = Address.builder()
                .street("Main St")
                .number("123")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .build();

        assertThat(address).isNotNull();
        assertThat(address.getStreet()).isEqualTo("Main St");
        assertThat(address.getFullAddress()).isEqualTo("Main St 123, New York, NY 10001, USA");
    }

    @Test
    @DisplayName("should handle optional fields")
    void shouldHandleOptionalFields() {
        Address address = Address.builder()
                .street("Main St")
                .number("123")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .apartment("Apt 4B")
                .build();

        assertThat(address.getApartment()).isEqualTo("Apt 4B");
        assertThat(address.getFullAddress()).contains("Apt 4B");
    }
}
