package com.adrianland.fundsorchestrator.unit.notification;

import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.Fund;
import com.adrianland.fundsorchestrator.domain.model.enums.FundCategory;
import com.adrianland.fundsorchestrator.domain.model.enums.NotificationType;
import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;
import com.adrianland.fundsorchestrator.infrastructure.notification.factory.NotificationStrategyFactory;
import com.adrianland.fundsorchestrator.infrastructure.notification.strategy.EmailNotificationStrategy;
import com.adrianland.fundsorchestrator.infrastructure.notification.strategy.NotificationStrategy;
import com.adrianland.fundsorchestrator.infrastructure.notification.strategy.SmsNotificationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationStrategyFactory – Unit Tests")
class NotificationStrategyFactoryTest {

    @Mock private EmailNotificationStrategy emailStrategy;
    @Mock private SmsNotificationStrategy smsStrategy;

    private NotificationStrategyFactory factory;
    private Client client;
    private Fund fund;

    @BeforeEach
    void setUp() {
        factory = new NotificationStrategyFactory(emailStrategy, smsStrategy);

        client = Client.builder()
                .clientId("CLIENT-001").name("Demo").email("demo@btg.com")
                .phone("+573001234567").balance(BigDecimal.ZERO)
                .notificationPreference(NotificationType.EMAIL).build();

        fund = Fund.builder()
                .fundId("1").name("FPV_BTG_PACTUAL_RECAUDADORA")
                .minAmount(new BigDecimal("75000")).category(FundCategory.FPV).build();
    }

    @Test
    @DisplayName("Factory should return EmailStrategy for EMAIL preference")
    void shouldReturnEmailStrategy() {
        NotificationStrategy strategy = factory.getStrategy(NotificationType.EMAIL);
        assertThat(strategy).isSameAs(emailStrategy);
    }

    @Test
    @DisplayName("Factory should return SmsStrategy for SMS preference")
    void shouldReturnSmsStrategy() {
        NotificationStrategy strategy = factory.getStrategy(NotificationType.SMS);
        assertThat(strategy).isSameAs(smsStrategy);
    }

    @Test
    @DisplayName("EmailStrategy should be invoked with correct parameters")
    void shouldInvokeEmailStrategyWithCorrectArgs() {
        NotificationStrategy strategy = factory.getStrategy(NotificationType.EMAIL);
        strategy.send(client, fund, TransactionType.APERTURA);
        verify(emailStrategy).send(client, fund, TransactionType.APERTURA);
    }

    @Test
    @DisplayName("SmsStrategy should be invoked with correct parameters")
    void shouldInvokeSmsStrategyWithCorrectArgs() {
        NotificationStrategy strategy = factory.getStrategy(NotificationType.SMS);
        strategy.send(client, fund, TransactionType.APERTURA);
        verify(smsStrategy).send(client, fund, TransactionType.APERTURA);
    }
}
