package com.adrianland.fundsorchestrator.domain.exception;

public class NotSubscribedException extends RuntimeException {
    public NotSubscribedException(String clientId, String fundId) {
        super("Client " + clientId + " is not subscribed to fund " + fundId);
    }
}
