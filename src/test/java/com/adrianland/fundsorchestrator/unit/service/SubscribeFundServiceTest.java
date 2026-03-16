package com.adrianland.fundsorchestrator.unit.service;

import com.adrianland.fundsorchestrator.application.service.SubscribeFundService;
import com.adrianland.fundsorchestrator.application.service.TransactionFactory;
import com.adrianland.fundsorchestrator.domain.exception.*;
import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.Fund;
import com.adrianland.fundsorchestrator.domain.model.Transaction;
import com.adrianland.fundsorchestrator.domain.model.enums.FundCategory;
import com.adrianland.fundsorchestrator.domain.model.enums.NotificationType;
import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;
import com.adrianland.fundsorchestrator.domain.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscribeFundService – Unit Tests")
class SubscribeFundServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private FundRepository fundRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private IdempotencyRepository idempotencyRepository;
    @Mock private TransactionFactory transactionFactory;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SubscribeFundService service;

    private Client clientWith500k;
    private Client clientWith50k;
    private Fund fundWith75k;
    private Transaction fakeTx;

    @BeforeEach
    void setUp() {
        clientWith500k = Client.builder()
                .clientId("CLIENT-001")
                .name("Demo User")
                .email("demo@btg.com")
                .phone("+573001234567")
                .balance(new BigDecimal("500000"))
                .notificationPreference(NotificationType.EMAIL)
                .build();

        clientWith50k = Client.builder()
                .clientId("CLIENT-002")
                .name("Poor User")
                .email("poor@btg.com")
                .phone("+573009999999")
                .balance(new BigDecimal("50000"))
                .notificationPreference(NotificationType.SMS)
                .build();

        fundWith75k = Fund.builder()
                .fundId("1")
                .name("FPV_BTG_PACTUAL_RECAUDADORA")
                .minAmount(new BigDecimal("75000"))
                .category(FundCategory.FPV)
                .build();

        fakeTx = Transaction.builder()
                .txId(UUID.randomUUID().toString())
                .clientId("CLIENT-001")
                .fundId("1")
                .fundName("FPV_BTG_PACTUAL_RECAUDADORA")
                .type(TransactionType.APERTURA)
                .amount(new BigDecimal("75000"))
                .createdAt(Instant.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Should subscribe successfully and return APERTURA transaction")
        void shouldSubscribeSuccessfully() {
            when(idempotencyRepository.findTxIdByKey(any())).thenReturn(Optional.empty());
            when(clientRepository.findById("CLIENT-001")).thenReturn(Optional.of(clientWith500k));
            when(fundRepository.findById("1")).thenReturn(Optional.of(fundWith75k));
            when(subscriptionRepository.findByClientAndFund("CLIENT-001", "1"))
                    .thenReturn(Optional.empty());
            when(transactionFactory.createApertura(any(), any(), any(), any(), any()))
                    .thenReturn(fakeTx);

            Transaction result = service.execute("CLIENT-001", "1", "idempotency-key-123");

            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(TransactionType.APERTURA);
            assertThat(result.getAmount()).isEqualByComparingTo("75000");

            verify(subscriptionRepository).createSubscriptionAtomically(any(), any(), any(), any());
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Should return existing transaction on duplicate idempotency key")
        void shouldReturnExistingTxOnDuplicateKey() {
            String existingTxId = fakeTx.getTxId();
            when(idempotencyRepository.findTxIdByKey("dup-key")).thenReturn(Optional.of(existingTxId));
            when(transactionRepository.findByClientId("CLIENT-001")).thenReturn(List.of(fakeTx));

            Transaction result = service.execute("CLIENT-001", "1", "dup-key");

            assertThat(result.getTxId()).isEqualTo(existingTxId);
            verify(subscriptionRepository, never()).createSubscriptionAtomically(any(), any(), any(), any());
        }
    }



}
