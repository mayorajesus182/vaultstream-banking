package com.vaultstream.customer.infrastructure.persistence;

import com.vaultstream.customer.domain.model.Customer;
import com.vaultstream.customer.domain.model.CustomerStatus;
import com.vaultstream.customer.domain.repository.CustomerRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL implementation of CustomerRepository.
 * 
 * This is the infrastructure adapter that implements the domain port.
 */
@ApplicationScoped
public class CustomerRepositoryAdapter implements CustomerRepository {

    @Inject
    EntityManager em;

    @Inject
    CustomerMapper mapper;

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = em.find(CustomerEntity.class, customer.getId());

        if (entity != null) {
            // Update existing
            mapper.updateEntity(entity, customer);
            entity = em.merge(entity);
        } else {
            // Create new
            entity = mapper.toEntity(customer);
            em.persist(entity);
        }

        em.flush();
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        CustomerEntity entity = em.find(CustomerEntity.class, id);
        return Optional.ofNullable(entity).map(mapper::toDomain);
    }

    @Override
    public Optional<Customer> findByCustomerNumber(String customerNumber) {
        return em.createQuery("SELECT c FROM CustomerEntity c WHERE c.customerNumber = :num", CustomerEntity.class)
                .setParameter("num", customerNumber)
                .getResultStream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return em.createQuery("SELECT c FROM CustomerEntity c WHERE LOWER(c.email) = :email", CustomerEntity.class)
                .setParameter("email", email.toLowerCase())
                .getResultStream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Customer> findByNationalId(String nationalId) {
        return em.createQuery("SELECT c FROM CustomerEntity c WHERE c.nationalId = :nid", CustomerEntity.class)
                .setParameter("nid", nationalId)
                .getResultStream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public List<Customer> findAll(int page, int size) {
        return em.createQuery("SELECT c FROM CustomerEntity c ORDER BY c.createdAt DESC", CustomerEntity.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Customer> findByStatus(CustomerStatus status) {
        return em.createQuery("SELECT c FROM CustomerEntity c WHERE c.status = :status", CustomerEntity.class)
                .setParameter("status", status)
                .getResultList()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Customer> searchByName(String name, int page, int size) {
        String searchPattern = "%" + name.toLowerCase() + "%";
        return em.createQuery(
                "SELECT c FROM CustomerEntity c WHERE LOWER(c.firstName) LIKE :name OR LOWER(c.lastName) LIKE :name",
                CustomerEntity.class)
                .setParameter("name", searchPattern)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = em.createQuery("SELECT COUNT(c) FROM CustomerEntity c WHERE LOWER(c.email) = :email", Long.class)
                .setParameter("email", email.toLowerCase())
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsByNationalId(String nationalId) {
        Long count = em.createQuery("SELECT COUNT(c) FROM CustomerEntity c WHERE c.nationalId = :nid", Long.class)
                .setParameter("nid", nationalId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public long count() {
        return em.createQuery("SELECT COUNT(c) FROM CustomerEntity c", Long.class)
                .getSingleResult();
    }

    @Override
    public long countByStatus(CustomerStatus status) {
        return em.createQuery("SELECT COUNT(c) FROM CustomerEntity c WHERE c.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    @Override
    public void deleteById(UUID id) {
        CustomerEntity entity = em.find(CustomerEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }
}
