package com.adrianland.fundsorchestrator.application.service;

import com.adrianland.fundsorchestrator.application.usecase.GetClientUseCase;
import com.adrianland.fundsorchestrator.domain.exception.ClientNotFoundException;
import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.port.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetClientService implements GetClientUseCase {

    private final ClientRepository clientRepository;

    @Override
    public Client execute(String clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
    }
}
