package com.adrianland.fundsorchestrator.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;


@Getter
@Builder
public class Subscription {

    private final String clientId;
    private final String fundId;
    private final String fundName;

    /** Amount locked at the time of subscription (= fund's minAmount at that moment). */
    private final BigDecimal amount;

    private final Instant subscribedAt;
}
