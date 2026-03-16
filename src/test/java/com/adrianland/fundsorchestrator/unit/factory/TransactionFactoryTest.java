package com.adrianland.fundsorchestrator.unit.factory;

import com.adrianland.fundsorchestrator.application.service.TransactionFactory;
import com.adrianland.fundsorchestrator.domain.model.Transaction;
import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionFactory – Unit Tests")
class TransactionFactoryTest {

    private final TransactionFactory factory = new TransactionFactory();

    @Test
    @DisplayName("createApertura should produce a valid APERTURA transaction with unique txId")
    void shouldCreateAperturaTransaction() {
        Transaction tx = factory.createApertura(
                "CLIENT-001", "1", "FPV_BTG_PACTUAL_RECAUDADORA",
                new BigDecimal("75000"), "idem-key-abc");

        assertThat(tx.getTxId()).isNotBlank();
        assertThat(tx.getType()).isEqualTo(TransactionType.APERTURA);
        assertThat(tx.getClientId()).isEqualTo("CLIENT-001");
        assertThat(tx.getFundId()).isEqualTo("1");
        assertThat(tx.getFundName()).isEqualTo("FPV_BTG_PACTUAL_RECAUDADORA");
        assertThat(tx.getAmount()).isEqualByComparingTo("75000");
        assertThat(tx.getIdempotencyKey()).isEqualTo("idem-key-abc");
        assertThat(tx.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("createCancelacion should produce a valid CANCELACION transaction")
    void shouldCreateCancelacionTransaction() {
        Transaction tx = factory.createCancelacion(
                "CLIENT-001", "1", "FPV_BTG_PACTUAL_RECAUDADORA",
                new BigDecimal("75000"));

        assertThat(tx.getTxId()).isNotBlank();
        assertThat(tx.getType()).isEqualTo(TransactionType.CANCELACION);
        assertThat(tx.getIdempotencyKey()).isNull();
    }

    @Test
    @DisplayName("Each transaction should have a globally unique txId")
    void shouldGenerateUniqueTxIds() {
        Transaction tx1 = factory.createApertura("C1", "1", "Fund", new BigDecimal("75000"), null);
        Transaction tx2 = factory.createApertura("C1", "1", "Fund", new BigDecimal("75000"), null);

        assertThat(tx1.getTxId()).isNotEqualTo(tx2.getTxId());
    }
}
