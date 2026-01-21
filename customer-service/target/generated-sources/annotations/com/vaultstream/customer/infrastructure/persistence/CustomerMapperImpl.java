package com.vaultstream.customer.infrastructure.persistence;

import com.vaultstream.customer.domain.model.Address;
import com.vaultstream.customer.domain.model.Customer;
import jakarta.enterprise.context.ApplicationScoped;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-21T17:00:10+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.8 (Ubuntu)"
)
@ApplicationScoped
public class CustomerMapperImpl implements CustomerMapper {

    @Override
    public Customer toDomain(CustomerEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Customer.CustomerBuilder customer = Customer.builder();

        customer.address( toAddress( entity ) );
        customer.id( entity.getId() );
        customer.customerNumber( entity.getCustomerNumber() );
        customer.firstName( entity.getFirstName() );
        customer.lastName( entity.getLastName() );
        customer.email( entity.getEmail() );
        customer.phoneNumber( entity.getPhoneNumber() );
        customer.dateOfBirth( entity.getDateOfBirth() );
        customer.nationalId( entity.getNationalId() );
        customer.status( entity.getStatus() );
        customer.type( entity.getType() );
        customer.createdAt( entity.getCreatedAt() );
        customer.updatedAt( entity.getUpdatedAt() );
        customer.version( entity.getVersion() );

        return customer.build();
    }

    @Override
    public CustomerEntity toEntity(Customer customer) {
        if ( customer == null ) {
            return null;
        }

        CustomerEntity.CustomerEntityBuilder customerEntity = CustomerEntity.builder();

        customerEntity.street( customerAddressStreet( customer ) );
        customerEntity.streetNumber( customerAddressNumber( customer ) );
        customerEntity.apartment( customerAddressApartment( customer ) );
        customerEntity.city( customerAddressCity( customer ) );
        customerEntity.state( customerAddressState( customer ) );
        customerEntity.postalCode( customerAddressPostalCode( customer ) );
        customerEntity.country( customerAddressCountry( customer ) );
        customerEntity.id( customer.getId() );
        customerEntity.customerNumber( customer.getCustomerNumber() );
        customerEntity.firstName( customer.getFirstName() );
        customerEntity.lastName( customer.getLastName() );
        customerEntity.email( customer.getEmail() );
        customerEntity.phoneNumber( customer.getPhoneNumber() );
        customerEntity.dateOfBirth( customer.getDateOfBirth() );
        customerEntity.nationalId( customer.getNationalId() );
        customerEntity.status( customer.getStatus() );
        customerEntity.type( customer.getType() );
        customerEntity.createdAt( customer.getCreatedAt() );
        customerEntity.updatedAt( customer.getUpdatedAt() );
        customerEntity.version( customer.getVersion() );

        return customerEntity.build();
    }

    @Override
    public void updateEntity(CustomerEntity entity, Customer customer) {
        if ( customer == null ) {
            return;
        }

        entity.setStreet( customerAddressStreet( customer ) );
        entity.setStreetNumber( customerAddressNumber( customer ) );
        entity.setApartment( customerAddressApartment( customer ) );
        entity.setCity( customerAddressCity( customer ) );
        entity.setState( customerAddressState( customer ) );
        entity.setPostalCode( customerAddressPostalCode( customer ) );
        entity.setCountry( customerAddressCountry( customer ) );
        entity.setId( customer.getId() );
        entity.setCustomerNumber( customer.getCustomerNumber() );
        entity.setFirstName( customer.getFirstName() );
        entity.setLastName( customer.getLastName() );
        entity.setEmail( customer.getEmail() );
        entity.setPhoneNumber( customer.getPhoneNumber() );
        entity.setDateOfBirth( customer.getDateOfBirth() );
        entity.setNationalId( customer.getNationalId() );
        entity.setStatus( customer.getStatus() );
        entity.setType( customer.getType() );
        entity.setCreatedAt( customer.getCreatedAt() );
        entity.setUpdatedAt( customer.getUpdatedAt() );
        entity.setVersion( customer.getVersion() );
    }

    private String customerAddressStreet(Customer customer) {
        Address address = customer.getAddress();
        if ( address == null ) {
            return null;
        }
        return address.getStreet();
    }

    private String customerAddressNumber(Customer customer) {
        Address address = customer.getAddress();
        if ( address == null ) {
            return null;
        }
        return address.getNumber();
    }

    private String customerAddressApartment(Customer customer) {
        Address address = customer.getAddress();
        if ( address == null ) {
            return null;
        }
        return address.getApartment();
    }

    private String customerAddressCity(Customer customer) {
        Address address = customer.getAddress();
        if ( address == null ) {
            return null;
        }
        return address.getCity();
    }

    private String customerAddressState(Customer customer) {
        Address address = customer.getAddress();
        if ( address == null ) {
            return null;
        }
        return address.getState();
    }

    private String customerAddressPostalCode(Customer customer) {
        Address address = customer.getAddress();
        if ( address == null ) {
            return null;
        }
        return address.getPostalCode();
    }

    private String customerAddressCountry(Customer customer) {
        Address address = customer.getAddress();
        if ( address == null ) {
            return null;
        }
        return address.getCountry();
    }
}
