package com.example.account.web.rest;

import com.example.account.service.AccountService;
import com.example.account.web.rest.AccountDtos.AccountResponse;
import com.example.account.web.rest.AccountDtos.BalanceOperationRequest;
import com.example.account.web.rest.AccountDtos.CreateAccountRequest;
import com.example.account.web.rest.AccountDtos.UpdateStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@RequestBody CreateAccountRequest request) {
        return AccountResponse.from(accountService.createAccount(
                request.customerReference(), request.currency(), request.balance(), request.status()));
    }

    @GetMapping
    public List<AccountResponse> list(@RequestParam(required = false) String customerReference) {
        return accountService.listAccounts(customerReference).stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return AccountResponse.from(accountService.getAccount(id));
    }

    @PatchMapping("/{id}/status")
    public AccountResponse updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        return AccountResponse.from(accountService.updateStatus(id, request.status()));
    }

    @PostMapping("/{id}/debit")
    public AccountResponse debit(@PathVariable UUID id, @RequestBody BalanceOperationRequest request) {
        return AccountResponse.from(accountService.debit(id, request.amount()));
    }

    @PostMapping("/{id}/credit")
    public AccountResponse credit(@PathVariable UUID id, @RequestBody BalanceOperationRequest request) {
        return AccountResponse.from(accountService.credit(id, request.amount()));
    }
}
