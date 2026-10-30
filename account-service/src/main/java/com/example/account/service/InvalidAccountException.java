package com.example.account.service;

/** Raised when a business validation on an account fails. */
public class InvalidAccountException extends RuntimeException {

    public InvalidAccountException(String message) {
        super(message);
    }
}
