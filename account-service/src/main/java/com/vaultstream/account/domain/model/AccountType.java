package com.vaultstream.account.domain.model;

/**
 * Enum representing the types of bank accounts.
 */
public enum AccountType {
    /**
     * Savings account - interest-bearing
     */
    SAVINGS,

    /**
     * Checking account - for daily transactions
     */
    CHECKING,

    /**
     * Money market account - higher interest
     */
    MONEY_MARKET,

    /**
     * Certificate of deposit - fixed term
     */
    CERTIFICATE_OF_DEPOSIT
}
