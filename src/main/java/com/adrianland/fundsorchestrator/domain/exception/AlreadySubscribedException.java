package com.adrianland.fundsorchestrator.domain.exception;

public class AlreadySubscribedException extends RuntimeException {
    public AlreadySubscribedException(String clientId, String fundId) {
        super("Client " + clientId + " is already subscribed to fund " + fundId);
    }
}
