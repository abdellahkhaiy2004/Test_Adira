package com.example.eventconsumer.event;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumption of {@code bank.transactions.created} against a real (embedded) broker: the event is received
 * and logged, a duplicate is detected, and a malformed message does not stop the listener.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, topics = TransactionEventConsumptionTest.TOPIC)
class TransactionEventConsumptionTest {

    static final String TOPIC = "bank.transactions.created";

    @Autowired
    EmbeddedKafkaBroker broker;
    @Autowired
    ReceivedEventStore store;

    @BeforeEach
    void reset() {
        store.clear();
    }

    /** Waits for the listener to catch up; consumption is asynchronous. */
    private static void awaitUntil(String description, BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 25_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("timed out waiting for: " + description);
    }

    private void publish(String key, String json) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        try (Producer<String, String> producer =
                     new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>(TOPIC, key, json));
            producer.flush();
        }
    }

    private static String event(UUID eventId, UUID transactionId, UUID accountId, String amount) {
        return """
                {"eventId":"%s","eventType":"TransactionCreated","transactionId":"%s","accountId":"%s",
                 "amount":%s,"currency":"EUR","occurredAt":"2026-01-15T10:30:00Z"}
                """.formatted(eventId, transactionId, accountId, amount);
    }

    @Test
    void eventIsConsumedAndRecordedWithoutTouchingBankingData() {
        UUID eventId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        publish(accountId.toString(), event(eventId, transactionId, accountId, "250.00"));

        awaitUntil("one consumed event", () -> store.recent().size() == 1);

        TransactionCreatedEvent received = store.recent().get(0);
        assertThat(received.eventId()).isEqualTo(eventId);
        assertThat(received.eventType()).isEqualTo("TransactionCreated");
        assertThat(received.transactionId()).isEqualTo(transactionId);
        assertThat(received.accountId()).isEqualTo(accountId);
        assertThat(received.amount()).isEqualByComparingTo("250.00");
        assertThat(received.currency()).isEqualTo("EUR");
        assertThat(received.occurredAt()).isEqualTo("2026-01-15T10:30:00Z");
    }

    @Test
    void redeliveryOfTheSameEventIsDetectedAsADuplicate() {
        UUID eventId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String json = event(eventId, UUID.randomUUID(), accountId, "10.00");

        publish(accountId.toString(), json);
        publish(accountId.toString(), json);

        awaitUntil("both deliveries consumed", () -> store.recent().size() == 2);
        assertThat(store.duplicateCount()).isEqualTo(1);
    }

    @Test
    void malformedMessageIsSkippedAndTheListenerKeepsConsuming() {
        UUID accountId = UUID.randomUUID();

        publish(accountId.toString(), "this is not json");
        publish(accountId.toString(), event(UUID.randomUUID(), UUID.randomUUID(), accountId, "7.50"));

        awaitUntil("the valid event after the malformed one", () -> store.recent().size() == 1);
        assertThat(store.recent().get(0).amount()).isEqualByComparingTo("7.50");
    }
}
