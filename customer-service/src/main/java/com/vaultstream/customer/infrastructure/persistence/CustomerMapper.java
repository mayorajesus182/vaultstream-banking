package com.vaultstream.customer.infrastructure.persistence;

import com.vaultstream.customer.domain.model.Address;
import com.vaultstream.customer.domain.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

/**
 * MapStruct mapper for Customer entity <-> domain model conversion.
 */
@Mapper(componentModel = "jakarta-cdi")
public interface CustomerMapper {

    // ========================================
    // Entity -> Domain
    // ========================================

    @Mapping(target = "address", source = "entity", qualifiedByName = "toAddress")
    Customer toDomain(CustomerEntity entity);

    @Named("toAddress")
    default Address toAddress(CustomerEntity entity) {
        if (entity.getStreet() == null && entity.getCity() == null) {
            return null;
        }
        return Address.builder()
                .street(entity.getStreet())
                .number(entity.getStreetNumber())
                .apartment(entity.getApartment())
                .city(entity.getCity())
                .state(entity.getState())
                .postalCode(entity.getPostalCode())
                .country(entity.getCountry())
                .build();
    }

    // ========================================
    // Domain -> Entity
    // ========================================

    @Mapping(target = "street", source = "address.street")
    @Mapping(target = "streetNumber", source = "address.number")
    @Mapping(target = "apartment", source = "address.apartment")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "state", source = "address.state")
    @Mapping(target = "postalCode", source = "address.postalCode")
    @Mapping(target = "country", source = "address.country")
    CustomerEntity toEntity(Customer customer);

    @Mapping(target = "street", source = "address.street")
    @Mapping(target = "streetNumber", source = "address.number")
    @Mapping(target = "apartment", source = "address.apartment")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "state", source = "address.state")
    @Mapping(target = "postalCode", source = "address.postalCode")
    @Mapping(target = "country", source = "address.country")
    void updateEntity(@MappingTarget CustomerEntity entity, Customer customer);
}
