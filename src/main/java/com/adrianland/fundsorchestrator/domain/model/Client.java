package com.adrianland.fundsorchestrator.domain.model;

import com.adrianland.fundsorchestrator.domain.model.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@With
public class Client {

    /** Unique identifier, e.g. {@code CLIENT-001} */
    private final String clientId;

    private final String name;
    private final String email;
    private final String phone;

    /** Current available balance in COP. Starts at 500 000 per business rule. */
    private final BigDecimal balance;

    private final NotificationType notificationPreference;

    // ── Balance helpers ──────────────────────────────────────────────────────

    public Client debit(BigDecimal amount) {
        return this.withBalance(this.balance.subtract(amount));
    }

    public Client credit(BigDecimal amount) {
        return this.withBalance(this.balance.add(amount));
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }
}
