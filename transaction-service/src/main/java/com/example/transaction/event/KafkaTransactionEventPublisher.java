package com.example.transaction.event;

import com.example.transaction.domain.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Sends {@link TransactionCreatedEvent} to the {@code bank.transactions.created} topic, keyed by
 * {@code accountId} so every event of one account lands on the same partition and keeps its order.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "bank.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTransactionEventPublisher implements TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;
    private final String topic;

    public KafkaTransactionEventPublisher(KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate,
                                          @Value("${bank.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publishCreated(Transaction transaction) {
        TransactionCreatedEvent event = TransactionCreatedEvent.of(transaction);
        String key = transaction.getAccountId().toString();
        try {
            kafkaTemplate.send(topic, key, event).whenComplete((result, failure) -> {
                if (failure != null) {
                    // The transaction is already committed; the event is simply lost. Logged for replay.
                    log.error("EVENT NOT PUBLISHED topic={} key={} transactionId={} - banking data is committed "
                            + "and unaffected", topic, key, event.transactionId(), failure);
                } else {
                    log.info("Published {} eventId={} transactionId={} to {} with key {}", event.eventType(),
                            event.eventId(), event.transactionId(), topic, key);
                }
            });
        } catch (RuntimeException e) {
            log.error("EVENT NOT PUBLISHED topic={} key={} transactionId={}", topic, key,
                    event.transactionId(), e);
        }
    }
}
