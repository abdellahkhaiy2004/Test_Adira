package com.example.eventconsumer.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * This module's own copy of the published contract (exam part F). It is intentionally duplicated rather than
 * imported: the topic is the only coupling between the producer and this consumer.
 */
public record TransactionCreatedEvent(UUID eventId, String eventType, UUID transactionId, UUID accountId,
                                      BigDecimal amount, String currency, Instant occurredAt) {
}
