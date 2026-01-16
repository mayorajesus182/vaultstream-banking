package com.vaultstream.customer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Value Object: Customer Address
 * 
 * Represents a physical address. Immutable.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    private String street;
    private String number;
    private String apartment;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    /**
     * Get formatted full address
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();

        if (street != null) {
            sb.append(street);
        }
        if (number != null) {
            sb.append(" ").append(number);
        }
        if (apartment != null && !apartment.isBlank()) {
            sb.append(", Apt ").append(apartment);
        }
        if (city != null) {
            sb.append(", ").append(city);
        }
        if (state != null) {
            sb.append(", ").append(state);
        }
        if (postalCode != null) {
            sb.append(" ").append(postalCode);
        }
        if (country != null) {
            sb.append(", ").append(country);
        }

        return sb.toString().trim();
    }

    /**
     * Validate address is complete
     */
    public boolean isComplete() {
        return street != null && !street.isBlank()
                && city != null && !city.isBlank()
                && country != null && !country.isBlank();
    }
}
