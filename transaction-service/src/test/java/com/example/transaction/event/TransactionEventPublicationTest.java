package com.example.transaction.event;

import com.example.transaction.FakeAccountServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.transaction.service.TransactionService;
import com.example.transaction.service.TransferService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Publication of {@code bank.transactions.created} against a real (embedded) broker: checks the topic, the
 * message key, the event contract, and that nothing is published when the transaction is refused.
 */
@SpringBootTest(properties = {
        "bank.kafka.enabled=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = TransactionEventPublicationTest.TOPIC)
class TransactionEventPublicationTest {

    static final String TOPIC = "bank.transactions.created";

    private static final FakeAccountServer ACCOUNTS = new FakeAccountServer();

    @DynamicPropertySource
    static void accountServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("bank.account-service.url", ACCOUNTS::url);
    }

    @AfterAll
    static void stop() {
        ACCOUNTS.close();
    }

    @Autowired
    TransactionService transactionService;
    @Autowired
    TransferService transferService;
    @Autowired
    EmbeddedKafkaBroker broker;
    @Autowired
    ObjectMapper objectMapper;

    /** Plain Kafka client so the assertions do not depend on the producer's own serializers. */
    private Consumer<String, String> newConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new StringDeserializer()).createConsumer();
        consumer.subscribe(List.of(TOPIC));
        return consumer;
    }

    /**
     * Collects records whose key is one of {@code keys}, until {@code expected} of them arrive or the wait
     * runs out. Every test shares the embedded broker and the topic, and each consumer starts from the
     * earliest offset, so filtering by key is what keeps one test from seeing another test's events.
     */
    private List<ConsumerRecord<String, String>> drainKeys(Consumer<String, String> consumer, int expected,
                                                           Duration timeout, String... keys) {
        Set<String> wanted = Set.of(keys);
        List<ConsumerRecord<String, String>> matching = new ArrayList<>();
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (matching.size() < expected && System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> batch = consumer.poll(Duration.ofMillis(500));
            batch.records(TOPIC).forEach(record -> {
                if (wanted.contains(record.key())) {
                    matching.add(record);
                }
            });
        }
        return matching;
    }

    @Test
    void transactionPublishesEventKeyedByAccountIdWithTheOfficialContract() throws Exception {
        UUID account = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");

        try (Consumer<String, String> consumer = newConsumer()) {
            var saved = transactionService.createTransaction(account, new BigDecimal("250.00"), "EUR",
                    "GROCERY", "MA");

            List<ConsumerRecord<String, String>> records =
                    drainKeys(consumer, 1, Duration.ofSeconds(20), account.toString());
            assertThat(records).hasSize(1);

            ConsumerRecord<String, String> record = records.get(0);
            assertThat(record.key()).as("message key must be the accountId").isEqualTo(account.toString());

            JsonNode event = objectMapper.readTree(record.value());
            assertThat(event.fieldNames()).toIterable().containsExactlyInAnyOrder(
                    "eventId", "eventType", "transactionId", "accountId", "amount", "currency", "occurredAt");
            assertThat(event.get("eventType").asText()).isEqualTo("TransactionCreated");
            assertThat(event.get("transactionId").asText()).isEqualTo(saved.getId().toString());
            assertThat(event.get("accountId").asText()).isEqualTo(account.toString());
            assertThat(new BigDecimal(event.get("amount").asText())).isEqualByComparingTo("250.00");
            assertThat(event.get("currency").asText()).isEqualTo("EUR");
            assertThat(UUID.fromString(event.get("eventId").asText())).isNotNull();
            // ISO-8601 instant, e.g. 2026-01-15T10:30:00Z - not an epoch number.
            assertThat(event.get("occurredAt").asText()).matches("\\d{4}-\\d{2}-\\d{2}T.*Z");
        }
    }

    @Test
    void rejectedTransactionPublishesNothing() {
        UUID poor = ACCOUNTS.addAccount("ACTIVE", "EUR", "5");

        try (Consumer<String, String> consumer = newConsumer()) {
            assertThatThrownBy(() -> transactionService.createTransaction(poor, new BigDecimal("100"), "EUR",
                    "GROCERY", "MA")).isInstanceOf(RuntimeException.class);

            // Nothing may be emitted for a transaction that was never persisted.
            assertThat(drainKeys(consumer, 1, Duration.ofSeconds(5), poor.toString())).isEmpty();
        }
    }

    @Test
    void transferPublishesOneEventPerLegEachKeyedByItsOwnAccount() {
        UUID from = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        UUID to = ACCOUNTS.addAccount("ACTIVE", "EUR", "100");

        try (Consumer<String, String> consumer = newConsumer()) {
            transferService.transfer(from, to, new BigDecimal("200"), "EUR", "MA");

            List<ConsumerRecord<String, String>> records =
                    drainKeys(consumer, 2, Duration.ofSeconds(20), from.toString(), to.toString());

            assertThat(records).hasSize(2);
            assertThat(records).extracting(ConsumerRecord::key)
                    .containsExactlyInAnyOrder(from.toString(), to.toString());
        }
    }
}
