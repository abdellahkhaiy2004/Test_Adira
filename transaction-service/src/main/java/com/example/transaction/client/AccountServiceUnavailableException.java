package com.example.transaction.client;

/** account-service is down, too slow, failing (5xx) or the circuit breaker is open. */
public class AccountServiceUnavailableException extends RuntimeException {

    public AccountServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
