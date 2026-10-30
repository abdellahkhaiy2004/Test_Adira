package com.example.eventconsumer.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code bank.transactions.created} and logs each event. It deliberately performs no write of any
 * kind against accounts or transactions (exam part F.4).
 *
 * <p>The payload is taken as raw JSON and parsed here, so the consumer needs no shared class or type header
 * with the producer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventListener {

    private final ObjectMapper objectMapper;
    private final ReceivedEventStore store;

    @KafkaListener(topics = "${bank.kafka.topic}", groupId = "${bank.kafka.group-id}")
    public void onTransactionCreated(@Payload String payload,
                                     @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset) {
        TransactionCreatedEvent event;
        try {
            event = objectMapper.readValue(payload, TransactionCreatedEvent.class);
        } catch (Exception e) {
            // A malformed message must not stop the listener from draining the partition.
            log.error("Unreadable event skipped: partition={} offset={} key={} payload={}",
                    partition, offset, key, payload, e);
            return;
        }

        boolean firstTime = store.record(event);
        if (firstTime) {
            log.info("Received {} eventId={} transactionId={} accountId={} amount={} {} occurredAt={} "
                            + "(key={} partition={} offset={})",
                    event.eventType(), event.eventId(), event.transactionId(), event.accountId(),
                    event.amount(), event.currency(), event.occurredAt(), key, partition, offset);
        } else {
            log.warn("Duplicate {} eventId={} ignored for logging purposes (key={} partition={} offset={})",
                    event.eventType(), event.eventId(), key, partition, offset);
        }
    }
}
