package com.adrianland.fundsorchestrator.domain.event;

import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.Fund;
import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;
import org.springframework.context.ApplicationEvent;


public class FundTransactionEvent extends ApplicationEvent {

    private final Client client;
    private final Fund fund;
    private final TransactionType transactionType;

    public FundTransactionEvent(Object source, Client client, Fund fund, TransactionType transactionType) {
        super(source);
        this.client = client;
        this.fund = fund;
        this.transactionType = transactionType;
    }

    public Client getClient() { return client; }
    public Fund getFund() { return fund; }
    public TransactionType getTransactionType() { return transactionType; }
}
