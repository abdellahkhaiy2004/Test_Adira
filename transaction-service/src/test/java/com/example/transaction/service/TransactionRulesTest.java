package com.example.transaction.service;

import com.example.transaction.client.AccountClient.AccountView;
import com.example.transaction.service.TransactionRejectedException.Reason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionRulesTest {

    private final TransactionRules rules = new TransactionRules(new BankTransactionProperties(new BigDecimal("1000")));

    private static AccountView account(String status, String currency, double balance) {
        return new AccountView(UUID.randomUUID(), "C", status, currency, balance);
    }

    private static Reason reasonOf(Runnable action) {
        try {
            action.run();
        } catch (TransactionRejectedException e) {
            return e.getReason();
        }
        return null;
    }

    @Test
    void amountBoundaries() {
        assertThatCode(() -> rules.validateAmount(new BigDecimal("0.01"))).doesNotThrowAnyException();
        assertThatCode(() -> rules.validateAmount(new BigDecimal("1000"))).doesNotThrowAnyException();
        assertThatCode(() -> rules.validateAmount(new BigDecimal("10.500"))).doesNotThrowAnyException();
        assertThat(reasonOf(() -> rules.validateAmount(BigDecimal.ZERO))).isEqualTo(Reason.INVALID_REQUEST);
        assertThat(reasonOf(() -> rules.validateAmount(new BigDecimal("-1")))).isEqualTo(Reason.INVALID_REQUEST);
        assertThat(reasonOf(() -> rules.validateAmount(null))).isEqualTo(Reason.INVALID_REQUEST);
        assertThat(reasonOf(() -> rules.validateAmount(new BigDecimal("0.001")))).isEqualTo(Reason.INVALID_REQUEST);
        assertThat(reasonOf(() -> rules.validateAmount(new BigDecimal("1000.01")))).isEqualTo(Reason.AMOUNT_EXCEEDS_MAX);
    }

    @Test
    void ceilingComesFromConfigurationNotFromCode() {
        TransactionRules strict = new TransactionRules(new BankTransactionProperties(new BigDecimal("50")));

        assertThat(reasonOf(() -> strict.validateAmount(new BigDecimal("50.01")))).isEqualTo(Reason.AMOUNT_EXCEEDS_MAX);
        assertThatCode(() -> strict.validateAmount(new BigDecimal("50"))).doesNotThrowAnyException();
    }

    @Test
    void missingCeilingFailsFast() {
        assertThatThrownBy(() -> new TransactionRules(new BankTransactionProperties(null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void accountChecks() {
        BigDecimal ten = BigDecimal.TEN;
        assertThatCode(() -> rules.requireCanPay(account("ACTIVE", "EUR", 10), ten, "EUR")).doesNotThrowAnyException();
        assertThat(reasonOf(() -> rules.requireCanPay(account("BLOCKED", "EUR", 100), ten, "EUR")))
                .isEqualTo(Reason.ACCOUNT_NOT_ACTIVE);
        assertThat(reasonOf(() -> rules.requireCanPay(account("ACTIVE", "USD", 100), ten, "EUR")))
                .isEqualTo(Reason.CURRENCY_MISMATCH);
        assertThat(reasonOf(() -> rules.requireCanPay(account("ACTIVE", "EUR", 9.99), ten, "EUR")))
                .isEqualTo(Reason.INSUFFICIENT_FUNDS);
        assertThatCode(() -> rules.requireCanReceive(account("ACTIVE", "EUR", 0), "EUR")).doesNotThrowAnyException();
    }

    @Test
    void currencyFormat() {
        assertThatCode(() -> rules.validateCurrency("EUR")).doesNotThrowAnyException();
        assertThat(reasonOf(() -> rules.validateCurrency("eur"))).isEqualTo(Reason.INVALID_REQUEST);
        assertThat(reasonOf(() -> rules.validateCurrency(null))).isEqualTo(Reason.INVALID_REQUEST);
    }
}
