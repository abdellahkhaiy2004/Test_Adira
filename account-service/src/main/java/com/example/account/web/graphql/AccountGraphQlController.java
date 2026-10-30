package com.example.account.web.graphql;

import com.example.account.domain.Account;
import com.example.account.domain.AccountStatus;
import com.example.account.service.AccountNotFoundException;
import com.example.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * GraphQL entry point. It only adapts arguments and delegates: every rule lives in {@link AccountService},
 * exactly as for the REST controller.
 */
@Controller
@RequiredArgsConstructor
public class AccountGraphQlController {

    private final AccountService accountService;

    @QueryMapping
    public List<Account> accounts(@Argument String customerReference) {
        return accountService.listAccounts(customerReference);
    }

    /** Returns null rather than an error when the account is unknown, as the schema allows. */
    @QueryMapping
    public Account account(@Argument UUID id) {
        try {
            return accountService.getAccount(id);
        } catch (AccountNotFoundException e) {
            return null;
        }
    }

    @MutationMapping
    public Account createAccount(@Argument String customerReference, @Argument String currency,
                                 @Argument Double balance, @Argument AccountStatus status) {
        return accountService.createAccount(customerReference, currency, balance, status);
    }

    @MutationMapping
    public Account updateAccountStatus(@Argument UUID id, @Argument AccountStatus status) {
        return accountService.updateStatus(id, status);
    }

    @MutationMapping
    public Account debitAccount(@Argument UUID id, @Argument BigDecimal amount) {
        return accountService.debit(id, amount);
    }

    @MutationMapping
    public Account creditAccount(@Argument UUID id, @Argument BigDecimal amount) {
        return accountService.credit(id, amount);
    }
}
