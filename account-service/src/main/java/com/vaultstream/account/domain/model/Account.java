package com.vaultstream.account.domain.model;

import com.vaultstream.account.domain.event.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Account Aggregate Root implementing Event Sourcing.
 * 
 * State is derived from applying domain events.
 * All changes are captured as events.
 */
@Slf4j
@Getter
public class Account {

    private UUID id;
    private String accountNumber;
    private UUID customerId;
    private Money balance;
    private AccountType type;
    private AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    // Uncommitted events to be persisted
    private final List<AccountEvent> uncommittedEvents = new ArrayList<>();

    /**
     * Private constructor - use factory methods
     */
    private Account() {
    }

    // ========================================
    // Factory Methods
    // ========================================

    /**
     * Create a new account from a creation command
     */
    public static Account create(
            String accountNumber,
            UUID customerId,
            AccountType type,
            Money initialBalance) {

        Account account = new Account();
        
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .accountId(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(customerId)
                .accountType(type)
                .status(AccountStatus.PENDING)
                .initialBalance(initialBalance.amount())
                .currency(initialBalance.currency())
                .build();

        account.apply(event);
        account.uncommittedEvents.add(event);

        log.info("Account created: {} for customer: {}", accountNumber, customerId);
        return account;
    }

    /**
     * Reconstruct an account from its event history
     */
    public static Account fromHistory(List<AccountEvent> events) {
        Account account = new Account();
        for (AccountEvent event : events) {
            account.apply(event);
            account.version++;
        }
        return account;
    }

    // ========================================
    // Commands (Business Operations)
    // ========================================

    /**
     * Activate the account
     */
    public void activate() {
        validateStatusTransition(AccountStatus.ACTIVE);

        AccountStatusChangedEvent event = AccountStatusChangedEvent.builder()
                .accountId(id)
                .previousStatus(status)
                .newStatus(AccountStatus.ACTIVE)
                .reason("Account activated")
                .build();

        apply(event);
        uncommittedEvents.add(event);
    }

    /**
     * Deposit money into the account
     */
    public void deposit(Money amount, String description, String transactionRef) {
        validateActiveStatus();
        validatePositiveAmount(amount);
        validateSameCurrency(amount);

        Money newBalance = balance.add(amount);

        MoneyDepositedEvent event = MoneyDepositedEvent.builder()
                .accountId(id)
                .amount(amount.amount())
                .balanceAfter(newBalance.amount())
                .description(description)
                .transactionReference(transactionRef)
                .build();

        apply(event);
        uncommittedEvents.add(event);
    }

    /**
     * Withdraw money from the account
     */
    public void withdraw(Money amount, String description, String transactionRef) {
        validateActiveStatus();
        validatePositiveAmount(amount);
        validateSameCurrency(amount);
        validateSufficientFunds(amount);

        Money newBalance = balance.subtract(amount);

        MoneyWithdrawnEvent event = MoneyWithdrawnEvent.builder()
                .accountId(id)
                .amount(amount.amount())
                .balanceAfter(newBalance.amount())
                .description(description)
                .transactionReference(transactionRef)
                .build();

        apply(event);
        uncommittedEvents.add(event);
    }

    /**
     * Freeze the account
     */
    public void freeze(String reason) {
        validateStatusTransition(AccountStatus.FROZEN);

        AccountStatusChangedEvent event = AccountStatusChangedEvent.builder()
                .accountId(id)
                .previousStatus(status)
                .newStatus(AccountStatus.FROZEN)
                .reason(reason)
                .build();

        apply(event);
        uncommittedEvents.add(event);
    }

    /**
     * Close the account
     */
    public void close(String reason) {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account is already closed");
        }
        if (!balance.isZero()) {
            throw new IllegalStateException("Cannot close account with non-zero balance");
        }

        AccountStatusChangedEvent event = AccountStatusChangedEvent.builder()
                .accountId(id)
                .previousStatus(status)
                .newStatus(AccountStatus.CLOSED)
                .reason(reason)
                .build();

        apply(event);
        uncommittedEvents.add(event);
    }

    // ========================================
    // Event Handlers (Apply Events to State)
    // ========================================

    private void apply(AccountEvent event) {
        switch (event) {
            case AccountCreatedEvent e -> applyAccountCreated(e);
            case MoneyDepositedEvent e -> applyMoneyDeposited(e);
            case MoneyWithdrawnEvent e -> applyMoneyWithdrawn(e);
            case AccountStatusChangedEvent e -> applyStatusChanged(e);
            default -> throw new IllegalArgumentException("Unknown event type: " + event.getClass());
        }
        updatedAt = event.getOccurredAt();
    }

    private void applyAccountCreated(AccountCreatedEvent event) {
        this.id = event.getAccountId();
        this.accountNumber = event.getAccountNumber();
        this.customerId = event.getCustomerId();
        this.type = event.getAccountType();
        this.status = event.getStatus();
        this.balance = Money.of(event.getInitialBalance(), event.getCurrency());
        this.createdAt = event.getOccurredAt();
    }

    private void applyMoneyDeposited(MoneyDepositedEvent event) {
        this.balance = Money.of(event.getBalanceAfter(), balance.currency());
    }

    private void applyMoneyWithdrawn(MoneyWithdrawnEvent event) {
        this.balance = Money.of(event.getBalanceAfter(), balance.currency());
    }

    private void applyStatusChanged(AccountStatusChangedEvent event) {
        this.status = event.getNewStatus();
    }

    // ========================================
    // Event Management
    // ========================================

    public List<AccountEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    // ========================================
    // Validation
    // ========================================

    private void validateActiveStatus() {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active. Current status: " + status);
        }
    }

    private void validatePositiveAmount(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private void validateSameCurrency(Money amount) {
        if (!amount.currency().equals(balance.currency())) {
            throw new IllegalArgumentException("Currency mismatch");
        }
    }

    private void validateSufficientFunds(Money amount) {
        if (!balance.isGreaterThanOrEqual(amount)) {
            throw new IllegalStateException("Insufficient funds");
        }
    }

    private void validateStatusTransition(AccountStatus targetStatus) {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot change status of closed account");
        }
        if (status == targetStatus) {
            throw new IllegalStateException("Account is already " + targetStatus);
        }
    }
}
