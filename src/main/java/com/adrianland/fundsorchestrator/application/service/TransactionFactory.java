package com.adrianland.fundsorchestrator.application.service;

import com.adrianland.fundsorchestrator.domain.model.Transaction;
import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class TransactionFactory {

    /**
     * Build a new transaction for a fund subscription (APERTURA).
     */
    public Transaction createApertura(String clientId,
                                      String fundId,
                                      String fundName,
                                      BigDecimal amount,
                                      String idempotencyKey) {
        return Transaction.builder()
                .txId(UUID.randomUUID().toString())
                .clientId(clientId)
                .fundId(fundId)
                .fundName(fundName)
                .type(TransactionType.APERTURA)
                .amount(amount)
                .createdAt(Instant.now())
                .idempotencyKey(idempotencyKey)
                .build();
    }

    /**
     * Build a new transaction for a fund cancellation (CANCELACION).
     */
    public Transaction createCancelacion(String clientId,
                                         String fundId,
                                         String fundName,
                                         BigDecimal amount) {
        return Transaction.builder()
                .txId(UUID.randomUUID().toString())
                .clientId(clientId)
                .fundId(fundId)
                .fundName(fundName)
                .type(TransactionType.CANCELACION)
                .amount(amount)
                .createdAt(Instant.now())
                .build();
    }
}
