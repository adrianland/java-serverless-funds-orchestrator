package com.adrianland.fundsorchestrator.domain.port;

import com.adrianland.fundsorchestrator.domain.model.Transaction;

import java.util.List;

public interface TransactionRepository {

    void save(Transaction transaction);

    List<Transaction> findByClientId(String clientId);
}
