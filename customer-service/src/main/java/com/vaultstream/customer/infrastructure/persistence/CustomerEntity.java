package com.vaultstream.customer.infrastructure.persistence;

import com.vaultstream.customer.domain.model.CustomerStatus;
import com.vaultstream.customer.domain.model.CustomerType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for Customer persistence.
 * 
 * This is an infrastructure concern, separate from the domain model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_number", columnList = "customerNumber", unique = true),
        @Index(name = "idx_customer_email", columnList = "email", unique = true),
        @Index(name = "idx_customer_national_id", columnList = "nationalId", unique = true),
        @Index(name = "idx_customer_status", columnList = "status"),
        @Index(name = "idx_customer_name", columnList = "firstName, lastName")
})
public class CustomerEntity extends PanacheEntityBase {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String customerNumber;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String phoneNumber;

    @Column
    private LocalDate dateOfBirth;

    @Column(nullable = false, unique = true, length = 50)
    private String nationalId;

    // Address fields (embedded)
    @Column(length = 255)
    private String street;

    @Column(length = 20)
    private String streetNumber;

    @Column(length = 20)
    private String apartment;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerType type;

    @Column(length = 500)
    private String suspensionReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private int version;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
