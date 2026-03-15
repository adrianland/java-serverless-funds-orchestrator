package com.adrianland.fundsorchestrator.domain.port;

import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.Subscription;
import com.adrianland.fundsorchestrator.domain.model.Transaction;

import java.util.List;
import java.util.Optional;


public interface SubscriptionRepository {

    Optional<Subscription> findByClientAndFund(String clientId, String fundId);

    List<Subscription> findByClientId(String clientId);

    /**
     * Atomically: persists the subscription, deducts balance, saves the transaction,
     * and records the idempotency key in a single DynamoDB TransactWriteItems call.
     */
    void createSubscriptionAtomically(Subscription subscription,
                                      Transaction transaction,
                                      Client updatedClient,
                                      String idempotencyKey);

    /**
     * Atomically: removes the subscription, credits balance, and saves the transaction.
     */
    void cancelSubscriptionAtomically(String clientId,
                                      String fundId,
                                      Transaction transaction,
                                      Client updatedClient);
}
