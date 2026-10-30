package com.example.transaction.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Producer wiring for {@link TransactionCreatedEvent}.
 *
 * <p>The key is a plain String ({@code accountId}) and the value is JSON produced by the application's own
 * {@link ObjectMapper}, so instants are written as ISO-8601 ({@code 2026-01-15T10:30:00Z}) exactly as the
 * event contract requires. Type headers are switched off so any consumer can read the message without
 * sharing classes with this module.
 */
@Configuration
@ConditionalOnProperty(name = "bank.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaProducerConfiguration {

    @Bean
    public ProducerFactory<String, TransactionCreatedEvent> transactionEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers, ObjectMapper objectMapper) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        JsonSerializer<TransactionCreatedEvent> valueSerializer = new JsonSerializer<>(objectMapper);
        valueSerializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, TransactionCreatedEvent> transactionEventKafkaTemplate(
            ProducerFactory<String, TransactionCreatedEvent> factory) {
        return new KafkaTemplate<>(factory);
    }
}
