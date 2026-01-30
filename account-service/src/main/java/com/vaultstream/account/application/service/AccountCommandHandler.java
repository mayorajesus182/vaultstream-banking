package com.vaultstream.account.application.service;

import com.vaultstream.account.application.command.CreateAccountCommand;
import com.vaultstream.account.application.command.DepositMoneyCommand;
import com.vaultstream.account.application.command.WithdrawMoneyCommand;
import com.vaultstream.account.application.dto.AccountDto;
import com.vaultstream.account.domain.model.Account;
import com.vaultstream.account.domain.model.Money;
import com.vaultstream.account.infrastructure.persistence.EventStore;
import com.vaultstream.common.exception.ResourceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.Currency;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Account Command Handler - CQRS Write Side.
 * 
 * Handles commands, coordinates with aggregates, and persists events.
 */
@Slf4j
@ApplicationScoped
public class AccountCommandHandler {

    private static final AtomicInteger accountCounter = new AtomicInteger(0);

    @Inject
    EventStore eventStore;

    /**
     * Create a new account
     */
    @Transactional
    public AccountDto createAccount(CreateAccountCommand command) {
        log.info("Creating account for customer: {}", command.getCustomerId());

        String accountNumber = generateAccountNumber();
        Currency currency = Currency.getInstance(command.getCurrency());
        Money initialBalance = Money.of(command.getInitialDeposit(), currency);

        Account account = Account.create(
                accountNumber,
                command.getCustomerId(),
                command.getAccountType(),
                initialBalance
        );

        // Activate the account immediately if there's an initial deposit
        if (initialBalance.isPositive()) {
            account.activate();
        }

        eventStore.saveEvents(account);

        log.info("Account created: {}", account.getAccountNumber());
        return AccountDto.fromAggregate(account);
    }

    /**
     * Deposit money into an account
     */
    @Transactional
    public AccountDto deposit(DepositMoneyCommand command) {
        log.info("Depositing {} to account: {}", command.getAmount(), command.getAccountId());

        Account account = loadAccount(command.getAccountId());
        Money amount = Money.of(command.getAmount(), account.getBalance().currency());

        account.deposit(amount, command.getDescription(), command.getTransactionReference());
        eventStore.saveEvents(account);

        log.info("Deposit successful. New balance: {}", account.getBalance().amount());
        return AccountDto.fromAggregate(account);
    }

    /**
     * Withdraw money from an account
     */
    @Transactional
    public AccountDto withdraw(WithdrawMoneyCommand command) {
        log.info("Withdrawing {} from account: {}", command.getAmount(), command.getAccountId());

        Account account = loadAccount(command.getAccountId());
        Money amount = Money.of(command.getAmount(), account.getBalance().currency());

        account.withdraw(amount, command.getDescription(), command.getTransactionReference());
        eventStore.saveEvents(account);

        log.info("Withdrawal successful. New balance: {}", account.getBalance().amount());
        return AccountDto.fromAggregate(account);
    }

    /**
     * Activate an account
     */
    @Transactional
    public AccountDto activateAccount(UUID accountId) {
        log.info("Activating account: {}", accountId);

        Account account = loadAccount(accountId);
        account.activate();
        eventStore.saveEvents(account);

        return AccountDto.fromAggregate(account);
    }

    /**
     * Freeze an account
     */
    @Transactional
    public AccountDto freezeAccount(UUID accountId, String reason) {
        log.info("Freezing account: {} - Reason: {}", accountId, reason);

        Account account = loadAccount(accountId);
        account.freeze(reason);
        eventStore.saveEvents(account);

        return AccountDto.fromAggregate(account);
    }

    /**
     * Close an account
     */
    @Transactional
    public void closeAccount(UUID accountId, String reason) {
        log.info("Closing account: {} - Reason: {}", accountId, reason);

        Account account = loadAccount(accountId);
        account.close(reason);
        eventStore.saveEvents(account);
    }

    private Account loadAccount(UUID accountId) {
        return eventStore.loadAggregate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId.toString()));
    }

    private String generateAccountNumber() {
        int counter = accountCounter.incrementAndGet();
        return String.format("ACC-%08d", counter);
    }
}
