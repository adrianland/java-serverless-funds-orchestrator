package com.adrianland.fundsorchestrator.application.service;

import com.adrianland.fundsorchestrator.application.usecase.GetTransactionHistoryUseCase;
import com.adrianland.fundsorchestrator.domain.exception.ClientNotFoundException;
import com.adrianland.fundsorchestrator.domain.model.Transaction;
import com.adrianland.fundsorchestrator.domain.port.ClientRepository;
import com.adrianland.fundsorchestrator.domain.port.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetTransactionHistoryService implements GetTransactionHistoryUseCase {

    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public List<Transaction> execute(String clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        List<Transaction> history = transactionRepository.findByClientId(clientId);
        log.info("event=history_queried clientId={} count={}", clientId, history.size());
        return history;
    }
}
