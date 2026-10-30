package com.example.transaction.web.rest;

import com.example.transaction.domain.Transaction;
import com.example.transaction.domain.Transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record CreateTransactionRequest(UUID accountId, BigDecimal amount, String currency,
                                           String merchantCategory, String country) {
    }

    public record TransactionResponse(UUID id, UUID accountId, BigDecimal amount, String currency,
                                      String merchantCategory, String country, Instant createdAt) {

        public static TransactionResponse from(Transaction t) {
            return new TransactionResponse(t.getId(), t.getAccountId(), t.getAmount(), t.getCurrency(),
                    t.getMerchantCategory(), t.getCountry(), t.getCreatedAt());
        }
    }

    public record TransferRequest(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount,
                                  String currency, String country) {
    }

    public record TransferResponse(UUID id, UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount,
                                   String currency, Instant createdAt) {

        public static TransferResponse from(Transfer t) {
            return new TransferResponse(t.getId(), t.getSourceAccountId(), t.getDestinationAccountId(),
                    t.getAmount(), t.getCurrency(), t.getCreatedAt());
        }
    }

    public record ErrorResponse(int status, String error, String code, String message) {
    }
}
