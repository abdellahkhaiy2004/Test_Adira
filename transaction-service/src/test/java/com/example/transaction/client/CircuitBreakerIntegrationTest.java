package com.example.transaction.client;

import com.example.transaction.FakeAccountServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Resilience4J behaviour of the real Feign client against a fake account-service. */
@SpringBootTest(properties = {"bank.resilience.minimum-calls=3", "bank.resilience.open-state-seconds=60"})
class CircuitBreakerIntegrationTest {

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
    AccountClient client;

    @Test
    void businessRejectionsNeverOpenTheCircuitButRepeatedFailuresDo() {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "10");

        // 404 / 409 are business answers: many of them must not trip the breaker.
        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> client.getAccount(UUID.randomUUID()))
                    .isInstanceOf(RemoteAccountNotFoundException.class);
            assertThatThrownBy(() -> client.debit(acc, new AccountClient.AmountRequest(new BigDecimal("999"))))
                    .isInstanceOf(RemoteBusinessException.class);
        }
        // debit has its own breaker, so this success does not enter getAccount's sliding window.
        assertThat(client.debit(acc, new AccountClient.AmountRequest(BigDecimal.ONE)).balance()).isEqualTo(9.0);

        // Real failures (HTTP 500) count; after 3 calls the circuit opens and calls stop reaching the server.
        ACCOUNTS.failEverything(true);
        int before = ACCOUNTS.requestCount();
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> client.getAccount(acc)).isInstanceOf(AccountServiceUnavailableException.class);
        }
        assertThat(ACCOUNTS.requestCount()).isEqualTo(before + 3);

        assertThatThrownBy(() -> client.getAccount(acc)).isInstanceOf(AccountServiceUnavailableException.class);
        assertThat(ACCOUNTS.requestCount()).as("open circuit short-circuits the call").isEqualTo(before + 3);
    }
}
