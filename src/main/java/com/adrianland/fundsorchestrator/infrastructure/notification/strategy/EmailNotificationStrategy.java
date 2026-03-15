package com.adrianland.fundsorchestrator.infrastructure.notification.strategy;

import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.Fund;
import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simulated Email notification.
 *
 * <p>In production this would delegate to Amazon SES or a similar service.
 * For now, we emit a structured log line that CloudWatch Logs Insights can query.</p>
 */
@Slf4j
@Component
public class EmailNotificationStrategy implements NotificationStrategy {

    @Override
    public void send(Client client, Fund fund, TransactionType transactionType) {
        log.info(
                "NOTIFICACIÓN ENVIADA [EMAIL]: Cliente '{}' {} fondo '{}' → {}",
                client.getName(),
                transactionType.getAction(),
                fund.getName(),
                client.getEmail()
        );
    }
}
