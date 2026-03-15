package com.adrianland.fundsorchestrator.infrastructure.notification.strategy;

import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.Fund;
import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class SmsNotificationStrategy implements NotificationStrategy {

    @Override
    public void send(Client client, Fund fund, TransactionType transactionType) {
        log.info(
                "NOTIFICACIÓN ENVIADA [SMS]: Cliente '{}' {} fondo '{}' → {}",
                client.getName(),
                transactionType.getAction(),
                fund.getName(),
                client.getPhone()
        );
    }
}
