package com.example.transaction.web.rest;

import com.example.transaction.service.TransactionService;
import com.example.transaction.web.rest.TransactionDtos.CreateTransactionRequest;
import com.example.transaction.web.rest.TransactionDtos.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@RequestBody CreateTransactionRequest r) {
        return TransactionResponse.from(transactionService.createTransaction(
                r.accountId(), r.amount(), r.currency(), r.merchantCategory(), r.country()));
    }

    @GetMapping
    public List<TransactionResponse> history(@RequestParam(required = false) UUID accountId) {
        return transactionService.getHistory(accountId).stream().map(TransactionResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable UUID id) {
        return TransactionResponse.from(transactionService.getTransaction(id));
    }
}
