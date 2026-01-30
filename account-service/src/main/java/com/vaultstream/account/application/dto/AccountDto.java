package com.vaultstream.account.application.dto;

import com.vaultstream.account.domain.model.Account;
import com.vaultstream.account.domain.model.AccountStatus;
import com.vaultstream.account.domain.model.AccountType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

/**
 * DTO for Account read operations (Query side of CQRS).
 */
@Value
@Builder
public class AccountDto {

    String id;
    String accountNumber;
    String customerId;
    AccountType type;
    AccountStatus status;
    BigDecimal balance;
    String currency;
    Instant createdAt;
    Instant updatedAt;

    public static AccountDto fromAggregate(Account account) {
        return AccountDto.builder()
                .id(account.getId().toString())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId().toString())
                .type(account.getType())
                .status(account.getStatus())
                .balance(account.getBalance().amount())
                .currency(account.getBalance().currency().getCurrencyCode())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
