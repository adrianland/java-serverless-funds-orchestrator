package com.adrianland.fundsorchestrator.domain.exception;

public class FundNotFoundException extends RuntimeException {
    public FundNotFoundException(String fundId) {
        super("Fund not found: " + fundId);
    }
}
