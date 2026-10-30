package com.example.transaction.event;

import com.example.transaction.domain.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fallback used when {@code bank.kafka.enabled=false}, so the banking services stay usable with no broker
 * running. It only logs: it does not pretend the event reached a topic.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "bank.kafka.enabled", havingValue = "false")
public class LoggingTransactionEventPublisher implements TransactionEventPublisher {

    @Override
    public void publishCreated(Transaction transaction) {
        log.info("Kafka disabled - {} for transactionId={} accountId={} was NOT published",
                TransactionCreatedEvent.EVENT_TYPE, transaction.getId(), transaction.getAccountId());
    }
}
