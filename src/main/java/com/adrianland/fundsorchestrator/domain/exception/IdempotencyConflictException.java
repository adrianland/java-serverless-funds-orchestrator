package com.adrianland.fundsorchestrator.domain.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String key) {
        super("Idempotency key already used: " + key);
    }
}
