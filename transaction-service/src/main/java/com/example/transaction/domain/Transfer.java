package com.example.transaction.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Account-to-account transfer record. Not part of the official minimal model; it only links the two
 * {@link Transaction} rows (TRANSFER_OUT / TRANSFER_IN) a transfer produces.
 */
@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID sourceAccountId;

    private UUID destinationAccountId;

    private BigDecimal amount;

    private String currency;

    private Instant createdAt;
}
