package com.vaultstream.customer.domain.repository;

import com.vaultstream.customer.domain.model.Customer;
import com.vaultstream.customer.domain.model.CustomerStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Customer Repository Port (Domain Interface).
 * 
 * This is the contract that the domain requires from persistence.
 * The implementation is in the infrastructure layer.
 */
public interface CustomerRepository {

    /**
     * Save a customer (create or update)
     */
    Customer save(Customer customer);

    /**
     * Find customer by ID
     */
    Optional<Customer> findById(UUID id);

    /**
     * Find customer by customer number
     */
    Optional<Customer> findByCustomerNumber(String customerNumber);

    /**
     * Find customer by email
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Find customer by national ID
     */
    Optional<Customer> findByNationalId(String nationalId);

    /**
     * Find all customers with pagination
     */
    List<Customer> findAll(int page, int size);

    /**
     * Find customers by status
     */
    List<Customer> findByStatus(CustomerStatus status);

    /**
     * Search customers by name (first or last)
     */
    List<Customer> searchByName(String name, int page, int size);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if national ID exists
     */
    boolean existsByNationalId(String nationalId);

    /**
     * Check if customer number exists
     */
    boolean existsByCustomerNumber(String customerNumber);

    /**
     * Count total customers
     */
    long count();

    /**
     * Count customers by status
     */
    long countByStatus(CustomerStatus status);

    /**
     * Count customers matching name search
     */
    long countByNameSearch(String name);

    /**
     * Delete customer by ID
     */
    void deleteById(UUID id);
}
