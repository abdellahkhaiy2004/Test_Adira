package com.example.account.service;

import java.util.UUID;

public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException(UUID id) {
        super("Account " + id + " is not ACTIVE");
    }
}
