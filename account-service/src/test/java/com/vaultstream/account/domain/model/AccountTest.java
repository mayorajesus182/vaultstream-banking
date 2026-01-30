package com.vaultstream.account.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the Account aggregate root.
 */
@DisplayName("Account Aggregate")
class AccountTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create account with initial balance")
        void shouldCreateWithInitialBalance() {
            Account account = Account.create(
                    "ACC-001",
                    CUSTOMER_ID,
                    AccountType.SAVINGS,
                    Money.usd(BigDecimal.valueOf(100))
            );

            assertThat(account.getId()).isNotNull();
            assertThat(account.getAccountNumber()).isEqualTo("ACC-001");
            assertThat(account.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(account.getType()).isEqualTo(AccountType.SAVINGS);
            assertThat(account.getStatus()).isEqualTo(AccountStatus.PENDING);
            assertThat(account.getBalance().amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("should generate uncommitted events on creation")
        void shouldGenerateUncommittedEvents() {
            Account account = Account.create(
                    "ACC-002",
                    CUSTOMER_ID,
                    AccountType.CHECKING,
                    Money.zero(USD)
            );

            assertThat(account.getUncommittedEvents()).hasSize(1);
            assertThat(account.getUncommittedEvents().get(0).getEventType()).isEqualTo("AccountCreated");
        }
    }

    @Nested
    @DisplayName("Activation")
    class Activation {

        @Test
        @DisplayName("should activate pending account")
        void shouldActivatePendingAccount() {
            Account account = createPendingAccount();

            account.activate();

            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(account.getUncommittedEvents()).hasSize(2); // Created + StatusChanged
        }

        @Test
        @DisplayName("should throw when activating already active account")
        void shouldThrowWhenAlreadyActive() {
            Account account = createActiveAccount();

            assertThatThrownBy(() -> account.activate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already ACTIVE");
        }
    }

    @Nested
    @DisplayName("Deposit")
    class Deposit {

        @Test
        @DisplayName("should deposit money into active account")
        void shouldDepositMoney() {
            Account account = createActiveAccount();
            BigDecimal initialBalance = account.getBalance().amount();

            account.deposit(Money.usd(BigDecimal.valueOf(50)), "Test deposit", "TXN-001");

            assertThat(account.getBalance().amount())
                    .isEqualByComparingTo(initialBalance.add(BigDecimal.valueOf(50)));
        }

        @Test
        @DisplayName("should throw when depositing into non-active account")
        void shouldThrowWhenNotActive() {
            Account account = createPendingAccount();

            assertThatThrownBy(() -> 
                    account.deposit(Money.usd(BigDecimal.TEN), "Test", "TXN"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("should throw when depositing negative amount")
        void shouldThrowWhenNegativeAmount() {
            Account account = createActiveAccount();

            assertThatThrownBy(() -> 
                    account.deposit(Money.usd(BigDecimal.valueOf(-10)), "Test", "TXN"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }
    }

    @Nested
    @DisplayName("Withdrawal")
    class Withdrawal {

        @Test
        @DisplayName("should withdraw money from active account with sufficient funds")
        void shouldWithdrawMoney() {
            Account account = createActiveAccountWithBalance(BigDecimal.valueOf(100));

            account.withdraw(Money.usd(BigDecimal.valueOf(30)), "Test withdrawal", "TXN-002");

            assertThat(account.getBalance().amount()).isEqualByComparingTo(BigDecimal.valueOf(70));
        }

        @Test
        @DisplayName("should throw when withdrawing more than balance")
        void shouldThrowWhenInsufficientFunds() {
            Account account = createActiveAccountWithBalance(BigDecimal.valueOf(50));

            assertThatThrownBy(() -> 
                    account.withdraw(Money.usd(BigDecimal.valueOf(100)), "Test", "TXN"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient funds");
        }
    }

    @Nested
    @DisplayName("Freeze")
    class Freeze {

        @Test
        @DisplayName("should freeze active account")
        void shouldFreezeActiveAccount() {
            Account account = createActiveAccount();

            account.freeze("Suspicious activity");

            assertThat(account.getStatus()).isEqualTo(AccountStatus.FROZEN);
        }

        @Test
        @DisplayName("frozen account should not allow deposits")
        void frozenAccountShouldNotAllowDeposits() {
            Account account = createActiveAccount();
            account.freeze("Test freeze");

            assertThatThrownBy(() -> 
                    account.deposit(Money.usd(BigDecimal.TEN), "Test", "TXN"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Close")
    class Close {

        @Test
        @DisplayName("should close account with zero balance")
        void shouldCloseAccountWithZeroBalance() {
            Account account = createActiveAccountWithBalance(BigDecimal.ZERO);

            account.close("Customer request");

            assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        }

        @Test
        @DisplayName("should throw when closing account with non-zero balance")
        void shouldThrowWhenNonZeroBalance() {
            Account account = createActiveAccountWithBalance(BigDecimal.valueOf(100));

            assertThatThrownBy(() -> account.close("Test"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("non-zero balance");
        }
    }

    // Helper methods
    private Account createPendingAccount() {
        return Account.create("ACC-TEST", CUSTOMER_ID, AccountType.SAVINGS, Money.zero(USD));
    }

    private Account createActiveAccount() {
        Account account = createPendingAccount();
        account.activate();
        return account;
    }

    private Account createActiveAccountWithBalance(BigDecimal balance) {
        Account account = Account.create("ACC-TEST", CUSTOMER_ID, AccountType.SAVINGS, Money.usd(balance));
        account.activate();
        return account;
    }
}
