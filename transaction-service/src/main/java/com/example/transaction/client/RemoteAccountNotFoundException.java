package com.example.transaction.client;

public class RemoteAccountNotFoundException extends RuntimeException {

    public RemoteAccountNotFoundException(String message) {
        super(message);
    }
}
