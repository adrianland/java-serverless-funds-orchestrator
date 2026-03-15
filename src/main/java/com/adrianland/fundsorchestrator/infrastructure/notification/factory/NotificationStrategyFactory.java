package com.adrianland.fundsorchestrator.infrastructure.notification.factory;

import com.adrianland.fundsorchestrator.domain.model.enums.NotificationType;
import com.adrianland.fundsorchestrator.infrastructure.notification.strategy.EmailNotificationStrategy;
import com.adrianland.fundsorchestrator.infrastructure.notification.strategy.NotificationStrategy;
import com.adrianland.fundsorchestrator.infrastructure.notification.strategy.SmsNotificationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationStrategyFactory {

    private final EmailNotificationStrategy emailStrategy;
    private final SmsNotificationStrategy smsStrategy;

    public NotificationStrategy getStrategy(NotificationType type) {
        return switch (type) {
            case EMAIL -> emailStrategy;
            case SMS   -> smsStrategy;
        };
    }
}
