package com.example.transaction.web.rest;

import com.example.transaction.service.TransferService;
import com.example.transaction.web.rest.TransactionDtos.TransferRequest;
import com.example.transaction.web.rest.TransactionDtos.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(@RequestBody TransferRequest r) {
        return TransferResponse.from(transferService.transfer(
                r.sourceAccountId(), r.destinationAccountId(), r.amount(), r.currency(), r.country()));
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable UUID id) {
        return TransferResponse.from(transferService.getTransfer(id));
    }
}
