package com.adrianland.fundsorchestrator.application.usecase;

import com.adrianland.fundsorchestrator.domain.model.Transaction;

import java.util.List;

/** Retrieve the complete chronological list of transactions for a client. */
public interface GetTransactionHistoryUseCase {

    List<Transaction> execute(String clientId);
}
