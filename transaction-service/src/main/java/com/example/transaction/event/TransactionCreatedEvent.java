package com.example.transaction.event;

import com.example.transaction.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The official event contract (exam part F):
 * <pre>
 * {
 *   "eventId": "UUID",
 *   "eventType": "TransactionCreated",
 *   "transactionId": "UUID",
 *   "accountId": "UUID",
 *   "amount": 250.00,
 *   "currency": "EUR",
 *   "occurredAt": "2026-01-15T10:30:00Z"
 * }
 * </pre>
 * Field names and order follow the specification exactly.
 */
public record TransactionCreatedEvent(UUID eventId, String eventType, UUID transactionId, UUID accountId,
                                      BigDecimal amount, String currency, Instant occurredAt) {

    public static final String EVENT_TYPE = "TransactionCreated";

    /** Builds the event from a transaction that is already persisted. */
    public static TransactionCreatedEvent of(Transaction transaction) {
        return new TransactionCreatedEvent(UUID.randomUUID(), EVENT_TYPE, transaction.getId(),
                transaction.getAccountId(), transaction.getAmount(), transaction.getCurrency(),
                transaction.getCreatedAt());
    }
}
