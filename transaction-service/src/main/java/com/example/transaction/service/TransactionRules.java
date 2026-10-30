package com.example.transaction.service;

import com.example.transaction.client.AccountClient.AccountView;
import com.example.transaction.service.TransactionRejectedException.Reason;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Pattern;

/** The transaction business rules, shared by transactions and transfers so they are written once. */
@Component
public class TransactionRules {

    private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");
    private static final String ACTIVE = "ACTIVE";

    private final BigDecimal maxAmount;

    public TransactionRules(BankTransactionProperties properties) {
        if (properties.maxAmount() == null || properties.maxAmount().signum() <= 0) {
            throw new IllegalStateException("bank.transaction.max-amount must be configured and > 0");
        }
        this.maxAmount = properties.maxAmount();
    }

    /** Rules 1 and 2: strictly positive, at most 2 decimals (no silent rounding), within the ceiling. */
    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new TransactionRejectedException(Reason.INVALID_REQUEST, "amount must be strictly positive");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new TransactionRejectedException(Reason.INVALID_REQUEST, "amount must have at most 2 decimals");
        }
        if (amount.compareTo(maxAmount) > 0) {
            throw new TransactionRejectedException(Reason.AMOUNT_EXCEEDS_MAX,
                    "amount exceeds the maximum allowed (" + maxAmount.toPlainString() + ")");
        }
    }

    public void validateCurrency(String currency) {
        if (currency == null || !CURRENCY_CODE.matcher(currency).matches()) {
            throw new TransactionRejectedException(Reason.INVALID_REQUEST,
                    "currency must be a 3-letter uppercase code (e.g. EUR)");
        }
    }

    public void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new TransactionRejectedException(Reason.INVALID_REQUEST, field + " must not be blank");
        }
    }

    public void requireId(String field, UUID id) {
        if (id == null) {
            throw new TransactionRejectedException(Reason.INVALID_REQUEST, field + " is required");
        }
    }

    /** Rules 4, 5 and 6 for the account being debited: active, same currency, balance consulted. */
    public void requireCanPay(AccountView account, BigDecimal amount, String currency) {
        requireCanReceive(account, currency);
        if (BigDecimal.valueOf(account.balance()).compareTo(amount) < 0) {
            throw new TransactionRejectedException(Reason.INSUFFICIENT_FUNDS,
                    "insufficient funds on account " + account.id());
        }
    }

    /** Rules 4 and 5 for an account being credited. */
    public void requireCanReceive(AccountView account, String currency) {
        if (!ACTIVE.equals(account.status())) {
            throw new TransactionRejectedException(Reason.ACCOUNT_NOT_ACTIVE,
                    "account " + account.id() + " is not ACTIVE");
        }
        if (!currency.equals(account.currency())) {
            throw new TransactionRejectedException(Reason.CURRENCY_MISMATCH,
                    "currency " + currency + " does not match account " + account.id() + " currency "
                            + account.currency());
        }
    }
}
