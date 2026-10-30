package com.example.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Remote view of account-service. All business rules about balances live there; this is only transport.
 * The {@code url} is a temporary direct address until Eureka discovery is enabled (Part 3).
 */
@FeignClient(name = "account-service", url = "${bank.account-service.url}",
        configuration = AccountClientConfiguration.class, fallbackFactory = AccountClientFallbackFactory.class)
public interface AccountClient {

    @GetMapping("/api/accounts/{id}")
    AccountView getAccount(@PathVariable("id") UUID id);

    @PostMapping("/api/accounts/{id}/debit")
    AccountView debit(@PathVariable("id") UUID id, @RequestBody AmountRequest request);

    @PostMapping("/api/accounts/{id}/credit")
    AccountView credit(@PathVariable("id") UUID id, @RequestBody AmountRequest request);

    record AmountRequest(BigDecimal amount) {
    }

    record AccountView(UUID id, String customerReference, String status, String currency, double balance) {
    }
}
