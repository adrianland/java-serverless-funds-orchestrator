package com.adrianland.fundsorchestrator.application.usecase;

import com.adrianland.fundsorchestrator.domain.model.Transaction;

public interface SubscribeFundUseCase {

    Transaction execute(String clientId, String fundId, String idempotencyKey);
}
