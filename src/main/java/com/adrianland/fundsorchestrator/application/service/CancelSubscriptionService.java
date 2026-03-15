package com.adrianland.fundsorchestrator.application.service;

import com.adrianland.fundsorchestrator.application.usecase.CancelSubscriptionUseCase;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelSubscriptionService implements CancelSubscriptionUseCase {

    private final ClientRepository clientRepository;
    private final FundRepository fundRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionFactory transactionFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Transaction execute(String clientId, String fundId) {

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        Fund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new FundNotFoundException(fundId));

        Subscription subscription = subscriptionRepository
                .findByClientAndFund(clientId, fundId)
                .orElseThrow(() -> new NotSubscribedException(clientId, fundId));

        Transaction transaction = transactionFactory.createCancelacion(
                clientId, fundId, fund.getName(), subscription.getAmount());

        Client updatedClient = client.credit(subscription.getAmount());

        subscriptionRepository.cancelSubscriptionAtomically(
                clientId, fundId, transaction, updatedClient);

        log.info("event=fund_cancelled clientId={} fundId={} txId={} amount={}",
                clientId, fundId, transaction.getTxId(), subscription.getAmount());

        eventPublisher.publishEvent(
                new FundTransactionEvent(this, updatedClient, fund, transaction.getType()));

        return transaction;
    }
}
