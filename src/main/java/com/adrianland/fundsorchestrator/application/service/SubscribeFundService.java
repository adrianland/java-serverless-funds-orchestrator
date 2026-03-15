package com.adrianland.fundsorchestrator.application.service;

import com.adrianland.fundsorchestrator.application.usecase.SubscribeFundUseCase;
import com.adrianland.fundsorchestrator.domain.event.FundTransactionEvent;
import com.adrianland.fundsorchestrator.domain.exception.*;
import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.Fund;
import com.adrianland.fundsorchestrator.domain.model.Subscription;
import com.adrianland.fundsorchestrator.domain.model.Transaction;
import com.adrianland.fundsorchestrator.domain.port.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribeFundService implements SubscribeFundUseCase {

    private final ClientRepository clientRepository;
    private final FundRepository fundRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final TransactionFactory transactionFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Transaction execute(String clientId, String fundId, String idempotencyKey) {

        // ── 1. Idempotency check ────────────────────────────────────────────
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<String> existing = idempotencyRepository.findTxIdByKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("event=idempotency_hit clientId={} fundId={} idempotencyKey={}",
                        clientId, fundId, idempotencyKey);
                return transactionRepository.findByClientId(clientId)
                        .stream()
                        .filter(tx -> tx.getTxId().equals(existing.get()))
                        .findFirst()
                        .orElseThrow(() -> new IdempotencyConflictException(idempotencyKey));
            }
        }

        // ── 2. Load aggregates ──────────────────────────────────────────────
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        Fund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new FundNotFoundException(fundId));

        // ── 3. Guard: already subscribed ────────────────────────────────────
        subscriptionRepository.findByClientAndFund(clientId, fundId).ifPresent(s -> {
            throw new AlreadySubscribedException(clientId, fundId);
        });

        // ── 4. Guard: insufficient balance (BTG Pactual business rule) ───────
        if (!client.hasSufficientBalance(fund.getMinAmount())) {
            throw new InsufficientBalanceException(fund.getName());
        }

        // ── 5. Build domain objects ─────────────────────────────────────────
        Transaction transaction = transactionFactory.createApertura(
                clientId, fundId, fund.getName(), fund.getMinAmount(), idempotencyKey);

        Subscription subscription = Subscription.builder()
                .clientId(clientId)
                .fundId(fundId)
                .fundName(fund.getName())
                .amount(fund.getMinAmount())
                .subscribedAt(Instant.now())
                .build();

        Client updatedClient = client.debit(fund.getMinAmount());

        // ── 6. Persist atomically ────────────────────────────────────────────
        subscriptionRepository.createSubscriptionAtomically(
                subscription, transaction, updatedClient, idempotencyKey);

        log.info("event=fund_subscribed clientId={} fundId={} txId={} amount={}",
                clientId, fundId, transaction.getTxId(), fund.getMinAmount());

        // ── 7. Publish domain event (Observer → notification) ────────────────
        eventPublisher.publishEvent(new FundTransactionEvent(this, updatedClient, fund,
                transaction.getType()));

        return transaction;
    }
}
