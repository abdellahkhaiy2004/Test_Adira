package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountStatus;
import com.example.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");

    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount(String customerReference, String currency, Double initialBalance,
                                 AccountStatus status) {
        if (customerReference == null || customerReference.isBlank()) {
            throw new InvalidAccountException("customerReference must not be blank");
        }
        if (currency == null || !CURRENCY_CODE.matcher(currency).matches()) {
            throw new InvalidAccountException("currency must be a 3-letter uppercase code (e.g. EUR)");
        }
        double balance = initialBalance == null ? 0.0 : initialBalance;
        if (Double.isNaN(balance) || Double.isInfinite(balance) || balance < 0) {
            throw new InvalidAccountException("initial balance must be a finite number >= 0");
        }
        Account account = Account.builder()
                .customerReference(customerReference.trim())
                .currency(currency)
                .balance(balance)
                .status(status == null ? AccountStatus.ACTIVE : status)
                .build();
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID id) {
        return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Account> listAccounts(String customerReference) {
        if (customerReference == null || customerReference.isBlank()) {
            return accountRepository.findAll();
        }
        return accountRepository.findByCustomerReference(customerReference.trim());
    }

    @Transactional
    public Account updateStatus(UUID id, AccountStatus status) {
        if (status == null) {
            throw new InvalidAccountException("status is required (ACTIVE or BLOCKED)");
        }
        Account account = getAccount(id);
        account.setStatus(status);
        return accountRepository.save(account);
    }

    /** Removes {@code amount} from the balance. Account must be ACTIVE and funds sufficient. */
    @Transactional
    public Account debit(UUID id, BigDecimal amount) {
        requirePositive(amount);
        Account account = lockActive(id);
        BigDecimal newBalance = BigDecimal.valueOf(account.getBalance()).subtract(amount);
        if (newBalance.signum() < 0) {
            throw new InsufficientFundsException(id);
        }
        account.setBalance(newBalance.doubleValue());
        return accountRepository.save(account);
    }

    /** Adds {@code amount} to the balance. Account must be ACTIVE. */
    @Transactional
    public Account credit(UUID id, BigDecimal amount) {
        requirePositive(amount);
        Account account = lockActive(id);
        account.setBalance(BigDecimal.valueOf(account.getBalance()).add(amount).doubleValue());
        return accountRepository.save(account);
    }

    private Account lockActive(UUID id) {
        Account account = accountRepository.findByIdForUpdate(id).orElseThrow(() -> new AccountNotFoundException(id));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(id);
        }
        return account;
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAccountException("amount must be strictly positive");
        }
    }
}
