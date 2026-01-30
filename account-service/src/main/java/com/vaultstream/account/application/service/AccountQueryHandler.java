package com.vaultstream.account.application.service;

import com.vaultstream.account.application.dto.AccountDto;
import com.vaultstream.account.domain.model.Account;
import com.vaultstream.account.infrastructure.persistence.EventStore;
import com.vaultstream.common.dto.PageResponse;
import com.vaultstream.common.exception.ResourceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * Account Query Handler - CQRS Read Side.
 * 
 * Handles queries by loading aggregates from event store.
 * In a full CQRS implementation, this would read from projections.
 */
@Slf4j
@ApplicationScoped
public class AccountQueryHandler {

    @Inject
    EventStore eventStore;

    @Inject
    EntityManager em;

    /**
     * Get account by ID
     */
    public AccountDto getAccountById(UUID accountId) {
        Account account = eventStore.loadAggregate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId.toString()));
        return AccountDto.fromAggregate(account);
    }

    /**
     * Get account by account number
     */
    public AccountDto getAccountByNumber(String accountNumber) {
        UUID accountId = findAccountIdByNumber(accountNumber);
        return getAccountById(accountId);
    }

    /**
     * Get all accounts for a customer
     */
    public List<AccountDto> getAccountsByCustomerId(UUID customerId) {
        List<UUID> accountIds = findAccountIdsByCustomerId(customerId);
        return accountIds.stream()
                .map(id -> eventStore.loadAggregate(id))
                .filter(opt -> opt.isPresent())
                .map(opt -> AccountDto.fromAggregate(opt.get()))
                .toList();
    }

    /**
     * Get paginated list of all accounts
     */
    public PageResponse<AccountDto> getAllAccounts(int page, int size) {
        List<UUID> allAccountIds = findAllAccountIds();
        long total = allAccountIds.size();

        List<AccountDto> accounts = allAccountIds.stream()
                .skip((long) page * size)
                .limit(size)
                .map(id -> eventStore.loadAggregate(id))
                .filter(opt -> opt.isPresent())
                .map(opt -> AccountDto.fromAggregate(opt.get()))
                .toList();

        return PageResponse.of(accounts, page, size, total);
    }

    private UUID findAccountIdByNumber(String accountNumber) {
        // Query distinct aggregate IDs from event store for AccountCreated events
        List<UUID> results = em.createQuery(
                "SELECT DISTINCT e.aggregateId FROM EventStoreEntity e " +
                "WHERE e.eventType = 'AccountCreated' AND e.payload LIKE :pattern",
                UUID.class)
                .setParameter("pattern", "%\"accountNumber\":\"" + accountNumber + "\"%")
                .setMaxResults(1)
                .getResultList();

        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Account", accountNumber);
        }
        return results.get(0);
    }

    private List<UUID> findAccountIdsByCustomerId(UUID customerId) {
        return em.createQuery(
                "SELECT DISTINCT e.aggregateId FROM EventStoreEntity e " +
                "WHERE e.eventType = 'AccountCreated' AND e.payload LIKE :pattern",
                UUID.class)
                .setParameter("pattern", "%\"customerId\":\"" + customerId + "\"%")
                .getResultList();
    }

    private List<UUID> findAllAccountIds() {
        return em.createQuery(
                "SELECT DISTINCT e.aggregateId FROM EventStoreEntity e " +
                "WHERE e.eventType = 'AccountCreated'",
                UUID.class)
                .getResultList();
    }
}
