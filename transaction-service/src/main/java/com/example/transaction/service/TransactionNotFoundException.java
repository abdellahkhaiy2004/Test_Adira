package com.example.transaction.service;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String kind, UUID id) {
        super(kind + " not found: " + id);
    }
}
