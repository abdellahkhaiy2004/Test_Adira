package com.example.transaction.service;

import com.example.transaction.client.AccountClient;
import com.example.transaction.client.AccountClient.AccountView;
import com.example.transaction.client.AccountClient.AmountRequest;
import com.example.transaction.domain.Transaction;
import com.example.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final TransactionRules rules;
    private final TransactionTemplate transactionTemplate;

    /**
     * Records a payment (debit) on an account. Sequence: validate -> consult the account and its balance ->
     * persist the transaction and debit the balance as one unit; if the debit fails the insert is rolled back,
     * and if the local commit fails after a successful debit the debit is credited back.
     */
    public Transaction createTransaction(UUID accountId, BigDecimal amount, String currency,
                                         String merchantCategory, String country) {
        rules.requireId("accountId", accountId);
        rules.validateAmount(amount);
        rules.validateCurrency(currency);
        rules.requireText("merchantCategory", merchantCategory);
        rules.requireText("country", country);

        AccountView account = accountClient.getAccount(accountId);
        rules.requireCanPay(account, amount, currency);

        Compensations compensations = new Compensations();
        try {
            return transactionTemplate.execute(status -> {
                Transaction saved = transactionRepository.saveAndFlush(Transaction.builder()
                        .accountId(accountId).amount(amount).currency(currency)
                        .merchantCategory(merchantCategory.trim()).country(country.trim())
                        .createdAt(Instant.now()).build());
                accountClient.debit(accountId, new AmountRequest(amount));
                compensations.add(() -> accountClient.credit(accountId, new AmountRequest(amount)));
                return saved;
            });
        } catch (RuntimeException e) {
            compensations.runAll();
            throw e;
        }
    }

    public Transaction getTransaction(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction", id));
    }

    /** History, newest first. Without an accountId, returns every transaction. */
    public List<Transaction> getHistory(UUID accountId) {
        return accountId == null
                ? transactionRepository.findAllByOrderByCreatedAtDesc()
                : transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }
}
