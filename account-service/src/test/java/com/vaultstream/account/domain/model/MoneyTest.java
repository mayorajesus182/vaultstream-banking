package com.vaultstream.account.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the Money value object.
 */
@DisplayName("Money Value Object")
class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    @DisplayName("should create money with amount and currency")
    void shouldCreateMoney() {
        Money money = Money.of(BigDecimal.TEN, USD);

        assertThat(money.amount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(money.currency()).isEqualTo(USD);
    }

    @Test
    @DisplayName("should create USD money using factory method")
    void shouldCreateUsdMoney() {
        Money money = Money.usd(BigDecimal.valueOf(100));

        assertThat(money.currency()).isEqualTo(USD);
        assertThat(money.amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("should create zero money")
    void shouldCreateZeroMoney() {
        Money money = Money.zero(USD);

        assertThat(money.isZero()).isTrue();
        assertThat(money.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should add money of same currency")
    void shouldAddMoney() {
        Money a = Money.usd(BigDecimal.valueOf(100));
        Money b = Money.usd(BigDecimal.valueOf(50));

        Money result = a.add(b);

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    @DisplayName("should subtract money of same currency")
    void shouldSubtractMoney() {
        Money a = Money.usd(BigDecimal.valueOf(100));
        Money b = Money.usd(BigDecimal.valueOf(30));

        Money result = a.subtract(b);

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(70));
    }

    @Test
    @DisplayName("should throw when adding different currencies")
    void shouldThrowWhenAddingDifferentCurrencies() {
        Money usd = Money.usd(BigDecimal.TEN);
        Money eur = Money.of(BigDecimal.TEN, EUR);

        assertThatThrownBy(() -> usd.add(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
    }

    @Test
    @DisplayName("should detect positive amount")
    void shouldDetectPositive() {
        Money positive = Money.usd(BigDecimal.ONE);
        Money zero = Money.zero(USD);
        Money negative = Money.usd(BigDecimal.valueOf(-1));

        assertThat(positive.isPositive()).isTrue();
        assertThat(zero.isPositive()).isFalse();
        assertThat(negative.isPositive()).isFalse();
    }

    @Test
    @DisplayName("should detect negative amount")
    void shouldDetectNegative() {
        Money negative = Money.usd(BigDecimal.valueOf(-1));

        assertThat(negative.isNegative()).isTrue();
    }

    @Test
    @DisplayName("should compare money amounts")
    void shouldCompareMoney() {
        Money hundred = Money.usd(BigDecimal.valueOf(100));
        Money fifty = Money.usd(BigDecimal.valueOf(50));

        assertThat(hundred.isGreaterThanOrEqual(fifty)).isTrue();
        assertThat(fifty.isGreaterThanOrEqual(hundred)).isFalse();
        assertThat(hundred.isGreaterThanOrEqual(hundred)).isTrue();
    }

    @Test
    @DisplayName("should throw when amount is null")
    void shouldThrowWhenAmountNull() {
        assertThatThrownBy(() -> Money.of(null, USD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("should throw when currency is null")
    void shouldThrowWhenCurrencyNull() {
        assertThatThrownBy(() -> Money.of(BigDecimal.TEN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }
}
