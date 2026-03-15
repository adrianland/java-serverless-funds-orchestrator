package com.adrianland.fundsorchestrator.application.usecase;

import com.adrianland.fundsorchestrator.domain.model.Client;

/** Retrieve client details including current balance. */
public interface GetClientUseCase {

    Client execute(String clientId);
}
