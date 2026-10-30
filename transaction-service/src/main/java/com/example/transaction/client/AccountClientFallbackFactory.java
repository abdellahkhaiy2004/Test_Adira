package com.example.transaction.client;

import com.example.transaction.client.AccountClient.AccountView;
import com.example.transaction.client.AccountClient.AmountRequest;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Called by Resilience4J when a Feign call fails. Business rejections pass through unchanged; everything else
 * (connection refused, timeout, 5xx, open circuit) becomes {@link AccountServiceUnavailableException}.
 * There is deliberately no "fake success" fallback: banking operations must never be simulated.
 */
@Component
public class AccountClientFallbackFactory implements FallbackFactory<AccountClient> {

    @Override
    public AccountClient create(Throwable cause) {
        RuntimeException failure = translate(cause);
        return new AccountClient() {
            @Override
            public AccountView getAccount(UUID id) {
                throw failure;
            }

            @Override
            public AccountView debit(UUID id, AmountRequest request) {
                throw failure;
            }

            @Override
            public AccountView credit(UUID id, AmountRequest request) {
                throw failure;
            }
        };
    }

    static RuntimeException translate(Throwable cause) {
        for (Throwable t = cause; t != null; t = t.getCause()) {
            if (t instanceof RemoteBusinessException || t instanceof RemoteAccountNotFoundException) {
                return (RuntimeException) t;
            }
        }
        return new AccountServiceUnavailableException(
                "account-service unavailable (" + cause.getClass().getSimpleName() + ")", cause);
    }
}
