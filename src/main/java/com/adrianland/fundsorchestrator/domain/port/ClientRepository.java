package com.adrianland.fundsorchestrator.domain.port;

import com.adrianland.fundsorchestrator.domain.model.Client;

import java.util.Optional;


public interface ClientRepository {

    Optional<Client> findById(String clientId);

    void save(Client client);
}
