package com.adrianland.fundsorchestrator.domain.model;

import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;


@Getter
@Builder
public class Transaction {

    /** Globally unique transaction identifier (UUID v4). */
    private final String txId;

    private final String clientId;
    private final String fundId;
    private final String fundName;
    private final TransactionType type;
    private final BigDecimal amount;
    private final Instant createdAt;

    /** Idempotency key that originated this transaction (optional). */
    private final String idempotencyKey;
}
