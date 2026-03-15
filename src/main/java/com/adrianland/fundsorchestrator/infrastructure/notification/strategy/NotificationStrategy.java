package com.adrianland.fundsorchestrator.infrastructure.notification.strategy;

import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.Fund;
import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;


public interface NotificationStrategy {

    void send(Client client, Fund fund, TransactionType transactionType);
}
