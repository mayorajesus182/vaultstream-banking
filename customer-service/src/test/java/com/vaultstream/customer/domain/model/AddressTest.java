package com.vaultstream.customer.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Address value object.
 */
@DisplayName("Address Value Object")
class AddressTest {

    @Test
    @DisplayName("should create address with all fields")
    void shouldCreateAddressWithAllFields() {
        // When
        Address address = Address.builder()
                .street("Main Street")
                .number("123")
                .apartment("4B")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .build();

        // Then
        assertThat(address.getStreet()).isEqualTo("Main Street");
        assertThat(address.getNumber()).isEqualTo("123");
        assertThat(address.getApartment()).isEqualTo("4B");
        assertThat(address.getCity()).isEqualTo("New York");
        assertThat(address.getState()).isEqualTo("NY");
        assertThat(address.getPostalCode()).isEqualTo("10001");
        assertThat(address.getCountry()).isEqualTo("USA");
    }

    @Test
    @DisplayName("should create address with minimal fields")
    void shouldCreateAddressWithMinimalFields() {
        // When
        Address address = Address.builder()
                .street("Oak Avenue")
                .city("Los Angeles")
                .country("USA")
                .build();

        // Then
        assertThat(address.getStreet()).isEqualTo("Oak Avenue");
        assertThat(address.getCity()).isEqualTo("Los Angeles");
        assertThat(address.getCountry()).isEqualTo("USA");
        assertThat(address.getNumber()).isNull();
        assertThat(address.getApartment()).isNull();
        assertThat(address.getState()).isNull();
        assertThat(address.getPostalCode()).isNull();
    }

    @Test
    @DisplayName("should allow null optional fields")
    void shouldAllowNullOptionalFields() {
        // When
        Address address = Address.builder()
                .street("Test Street")
                .city("Test City")
                .build();

        // Then
        assertThat(address).isNotNull();
        assertThat(address.getStreet()).isEqualTo("Test Street");
    }
}
