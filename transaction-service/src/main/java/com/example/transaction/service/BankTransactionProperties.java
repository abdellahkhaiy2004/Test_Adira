package com.example.transaction.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Binds {@code bank.transaction.*}. The ceiling comes from configuration, never from code. */
@ConfigurationProperties(prefix = "bank.transaction")
public record BankTransactionProperties(BigDecimal maxAmount) {
}
