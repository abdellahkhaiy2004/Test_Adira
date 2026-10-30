package com.example.transaction.web.graphql;

import com.example.transaction.domain.Transaction;
import com.example.transaction.domain.Transfer;
import com.example.transaction.service.TransactionNotFoundException;
import com.example.transaction.service.TransactionService;
import com.example.transaction.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * GraphQL entry point. It only adapts arguments and delegates: every rule lives in the service layer,
 * exactly as for the REST controllers.
 */
@Controller
@RequiredArgsConstructor
public class TransactionGraphQlController {

    private final TransactionService transactionService;
    private final TransferService transferService;

    @QueryMapping
    public List<Transaction> transactions(@Argument UUID accountId) {
        return transactionService.getHistory(accountId);
    }

    @QueryMapping
    public Transaction transaction(@Argument UUID id) {
        try {
            return transactionService.getTransaction(id);
        } catch (TransactionNotFoundException e) {
            return null;
        }
    }

    @QueryMapping
    public Transfer transfer(@Argument UUID id) {
        try {
            return transferService.getTransfer(id);
        } catch (TransactionNotFoundException e) {
            return null;
        }
    }

    @MutationMapping
    public Transaction createTransaction(@Argument UUID accountId, @Argument BigDecimal amount,
                                         @Argument String currency, @Argument String merchantCategory,
                                         @Argument String country) {
        return transactionService.createTransaction(accountId, amount, currency, merchantCategory, country);
    }

    @MutationMapping("transfer")
    public Transfer makeTransfer(@Argument UUID sourceAccountId, @Argument UUID destinationAccountId,
                                 @Argument BigDecimal amount, @Argument String currency,
                                 @Argument String country) {
        return transferService.transfer(sourceAccountId, destinationAccountId, amount, currency, country);
    }
}
