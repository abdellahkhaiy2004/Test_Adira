package com.example.transaction.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfiguration {

    /**
     * Default circuit breaker + time limiter for every Feign method. Business rejections (400/404/409) are
     * ignored so a customer's invalid request can never open the circuit.
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> circuitBreakerCustomizer(
            @Value("${bank.resilience.timeout-ms}") long timeoutMs,
            @Value("${bank.resilience.minimum-calls}") int minimumCalls,
            @Value("${bank.resilience.failure-rate-threshold}") float failureRate,
            @Value("${bank.resilience.open-state-seconds}") long openStateSeconds) {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(Math.max(10, minimumCalls))
                        .minimumNumberOfCalls(minimumCalls)
                        .failureRateThreshold(failureRate)
                        .waitDurationInOpenState(Duration.ofSeconds(openStateSeconds))
                        .ignoreExceptions(RemoteBusinessException.class, RemoteAccountNotFoundException.class)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofMillis(timeoutMs))
                        .build())
                .build());
    }
}
