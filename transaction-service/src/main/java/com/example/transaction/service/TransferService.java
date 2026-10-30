package com.example.transaction.service;

import com.example.transaction.client.AccountClient;
import com.example.transaction.client.AccountClient.AccountView;
import com.example.transaction.client.AccountClient.AmountRequest;
import com.example.transaction.domain.Transaction;
import com.example.transaction.domain.Transfer;
import com.example.transaction.repository.TransactionRepository;
import com.example.transaction.repository.TransferRepository;
import com.example.transaction.service.TransactionRejectedException.Reason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    public static final String CATEGORY_OUT = "TRANSFER_OUT";
    public static final String CATEGORY_IN = "TRANSFER_IN";

    private final TransferRepository transferRepository;
    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final TransactionRules rules;
    private final TransactionTemplate transactionTemplate;

    /**
     * Moves money between two accounts of the same currency. Debit source, credit destination, record the
     * transfer and one history row per account. A failure at any step undoes the steps already applied.
     */
    public Transfer transfer(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount,
                             String currency, String country) {
        rules.requireId("sourceAccountId", sourceAccountId);
        rules.requireId("destinationAccountId", destinationAccountId);
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new TransactionRejectedException(Reason.INVALID_REQUEST,
                    "source and destination accounts must differ");
        }
        rules.validateAmount(amount);
        rules.validateCurrency(currency);
        rules.requireText("country", country);

        AccountView source = accountClient.getAccount(sourceAccountId);
        AccountView destination = accountClient.getAccount(destinationAccountId);
        rules.requireCanPay(source, amount, currency);
        rules.requireCanReceive(destination, currency);

        Compensations compensations = new Compensations();
        try {
            return transactionTemplate.execute(status -> {
                Instant now = Instant.now();
                Transfer transfer = transferRepository.saveAndFlush(Transfer.builder()
                        .sourceAccountId(sourceAccountId).destinationAccountId(destinationAccountId)
                        .amount(amount).currency(currency).createdAt(now).build());
                transactionRepository.save(historyRow(sourceAccountId, amount, currency, CATEGORY_OUT, country, now));
                transactionRepository.saveAndFlush(
                        historyRow(destinationAccountId, amount, currency, CATEGORY_IN, country, now));

                accountClient.debit(sourceAccountId, new AmountRequest(amount));
                compensations.add(() -> accountClient.credit(sourceAccountId, new AmountRequest(amount)));
                accountClient.credit(destinationAccountId, new AmountRequest(amount));
                compensations.add(() -> accountClient.debit(destinationAccountId, new AmountRequest(amount)));
                return transfer;
            });
        } catch (RuntimeException e) {
            compensations.runAll();
            throw e;
        }
    }

    public Transfer getTransfer(UUID id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transfer", id));
    }

    private Transaction historyRow(UUID accountId, BigDecimal amount, String currency, String category,
                                   String country, Instant at) {
        return Transaction.builder().accountId(accountId).amount(amount).currency(currency)
                .merchantCategory(category).country(country.trim()).createdAt(at).build();
    }
}
