package com.example.account.web.rest;

import com.example.account.domain.Account;
import com.example.account.domain.AccountStatus;

import java.util.UUID;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record CreateAccountRequest(String customerReference, String currency, Double balance,
                                       AccountStatus status) {
    }

    public record UpdateStatusRequest(AccountStatus status) {
    }

    public record AccountResponse(UUID id, String customerReference, AccountStatus status, String currency,
                                  double balance) {

        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getId(), a.getCustomerReference(), a.getStatus(), a.getCurrency(),
                    a.getBalance());
        }
    }

    public record ErrorResponse(int status, String error, String message) {
    }
}
