package com.adrianland.fundsorchestrator.application.usecase;

import com.adrianland.fundsorchestrator.domain.model.Transaction;

/** Cancel an existing fund subscription and credit the amount back to the client. */
public interface CancelSubscriptionUseCase {

    Transaction execute(String clientId, String fundId);
}
