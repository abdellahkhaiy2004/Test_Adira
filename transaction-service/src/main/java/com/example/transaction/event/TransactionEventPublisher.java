package com.example.transaction.event;

import com.example.transaction.domain.Transaction;

/**
 * Publishes a business event for a transaction that is already committed.
 *
 * <p>Implementations must never throw: the banking data is persisted by the time they are called, so a
 * messaging failure cannot be allowed to undo it or to fail the caller's request.
 */
public interface TransactionEventPublisher {

    void publishCreated(Transaction transaction);
}
