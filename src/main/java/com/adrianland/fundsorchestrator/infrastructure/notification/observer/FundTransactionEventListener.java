package com.adrianland.fundsorchestrator.infrastructure.notification.observer;

import com.adrianland.fundsorchestrator.domain.event.FundTransactionEvent;
import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;
import com.adrianland.fundsorchestrator.infrastructure.notification.factory.NotificationStrategyFactory;
import com.adrianland.fundsorchestrator.infrastructure.notification.strategy.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class FundTransactionEventListener {

    private final NotificationStrategyFactory strategyFactory;

    /**
     * Handles only APERTURA (subscription) events per business requirement.
     * Cancel events are also handled for completeness.
     */
    @Async
    @EventListener
    public void onFundTransaction(FundTransactionEvent event) {
        try {
            NotificationStrategy strategy = strategyFactory
                    .getStrategy(event.getClient().getNotificationPreference());

            strategy.send(event.getClient(), event.getFund(), event.getTransactionType());

        } catch (Exception ex) {
            // Notification failures must NEVER fail the main business flow
            log.error("event=notification_failed clientId={} fundId={} error={}",
                    event.getClient().getClientId(),
                    event.getFund().getFundId(),
                    ex.getMessage());
        }
    }
}
