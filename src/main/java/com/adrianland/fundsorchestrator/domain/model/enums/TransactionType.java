package com.adrianland.fundsorchestrator.domain.model.enums;

/**
 * Describes the nature of a fund transaction as per the BTG Pactual spec.
 */
public enum TransactionType {

    /** Opening / subscription to a fund. */
    APERTURA("suscrito al"),

    /** Cancellation of an existing subscription. */
    CANCELACION("canceló la suscripción al");

    private final String action;

    TransactionType(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
