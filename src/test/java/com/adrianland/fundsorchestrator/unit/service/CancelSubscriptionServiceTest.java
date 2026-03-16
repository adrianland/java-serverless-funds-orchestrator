package com.adrianland.fundsorchestrator.unit.service;

import com.adrianland.fundsorchestrator.application.service.CancelSubscriptionService;
import com.adrianland.fundsorchestrator.application.service.TransactionFactory;
import com.adrianland.fundsorchestrator.domain.exception.NotSubscribedException;
import com.adrianland.fundsorchestrator.domain.model.*;
import com.adrianland.fundsorchestrator.domain.model.enums.FundCategory;
import com.adrianland.fundsorchestrator.domain.model.enums.NotificationType;
import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;
import com.adrianland.fundsorchestrator.domain.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelSubscriptionService – Unit Tests")
class CancelSubscriptionServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private FundRepository fundRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private TransactionFactory transactionFactory;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CancelSubscriptionService service;

    private Client client;
    private Fund fund;
    private Subscription subscription;
    private Transaction cancelTx;

    @BeforeEach
    void setUp() {
        client = Client.builder()
                .clientId("CLIENT-001").name("Demo").email("demo@btg.com")
                .phone("+573001234567").balance(new BigDecimal("425000"))
                .notificationPreference(NotificationType.EMAIL).build();

        fund = Fund.builder()
                .fundId("1").name("FPV_BTG_PACTUAL_RECAUDADORA")
                .minAmount(new BigDecimal("75000")).category(FundCategory.FPV).build();

        subscription = Subscription.builder()
                .clientId("CLIENT-001").fundId("1").fundName("FPV_BTG_PACTUAL_RECAUDADORA")
                .amount(new BigDecimal("75000")).subscribedAt(Instant.now()).build();

        cancelTx = Transaction.builder()
                .txId(UUID.randomUUID().toString()).clientId("CLIENT-001")
                .fundId("1").fundName("FPV_BTG_PACTUAL_RECAUDADORA")
                .type(TransactionType.CANCELACION).amount(new BigDecimal("75000"))
                .createdAt(Instant.now()).build();
    }

    @Test
    @DisplayName("Should cancel subscription and return CANCELACION transaction")
    void shouldCancelSuccessfully() {
        when(clientRepository.findById("CLIENT-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientAndFund("CLIENT-001", "1"))
                .thenReturn(Optional.of(subscription));
        when(transactionFactory.createCancelacion(any(), any(), any(), any()))
                .thenReturn(cancelTx);

        Transaction result = service.execute("CLIENT-001", "1");

        assertThat(result.getType()).isEqualTo(TransactionType.CANCELACION);
        assertThat(result.getAmount()).isEqualByComparingTo("75000");

        verify(subscriptionRepository).cancelSubscriptionAtomically(
                eq("CLIENT-001"), eq("1"), any(), any());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("Should throw NotSubscribedException when client is not subscribed")
    void shouldThrowWhenNotSubscribed() {
        when(clientRepository.findById("CLIENT-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientAndFund("CLIENT-001", "1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute("CLIENT-001", "1"))
                .isInstanceOf(NotSubscribedException.class);

        verify(subscriptionRepository, never()).cancelSubscriptionAtomically(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cancel should credit the locked amount back to the client")
    void shouldCreditAmountBackToClient() {
        when(clientRepository.findById("CLIENT-001")).thenReturn(Optional.of(client));
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientAndFund("CLIENT-001", "1"))
                .thenReturn(Optional.of(subscription));
        when(transactionFactory.createCancelacion(any(), any(), any(), any()))
                .thenReturn(cancelTx);

        service.execute("CLIENT-001", "1");

        // Verify the updated client passed to the repository has the balance credited
        verify(subscriptionRepository).cancelSubscriptionAtomically(
                any(), any(), any(),
                argThat(updatedClient ->
                        updatedClient.getBalance().compareTo(new BigDecimal("500000")) == 0));
    }
}
