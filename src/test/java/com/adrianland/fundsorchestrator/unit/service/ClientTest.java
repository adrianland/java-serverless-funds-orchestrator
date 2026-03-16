package com.adrianland.fundsorchestrator.unit.service;

import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.enums.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Client Domain Model – Unit Tests")
class ClientTest {

    private Client buildClient(String balance) {
        return Client.builder()
                .clientId("CLIENT-001").name("Test").email("test@btg.com")
                .phone("+57300").balance(new BigDecimal(balance))
                .notificationPreference(NotificationType.EMAIL).build();
    }

    @Test
    @DisplayName("debit() should subtract amount and return new immutable instance")
    void shouldDebitCorrectly() {
        Client client = buildClient("500000");
        Client debited = client.debit(new BigDecimal("75000"));

        assertThat(debited.getBalance()).isEqualByComparingTo("425000");
        // Original must be unchanged (immutability)
        assertThat(client.getBalance()).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("credit() should add amount and return new immutable instance")
    void shouldCreditCorrectly() {
        Client client = buildClient("425000");
        Client credited = client.credit(new BigDecimal("75000"));

        assertThat(credited.getBalance()).isEqualByComparingTo("500000");
        assertThat(client.getBalance()).isEqualByComparingTo("425000");
    }

    @Test
    @DisplayName("hasSufficientBalance() should return true when balance >= amount")
    void shouldReturnTrueWhenBalanceSufficient() {
        Client client = buildClient("500000");
        assertThat(client.hasSufficientBalance(new BigDecimal("500000"))).isTrue();
        assertThat(client.hasSufficientBalance(new BigDecimal("499999"))).isTrue();
    }

    @Test
    @DisplayName("hasSufficientBalance() should return false when balance < amount")
    void shouldReturnFalseWhenBalanceInsufficient() {
        Client client = buildClient("74999");
        assertThat(client.hasSufficientBalance(new BigDecimal("75000"))).isFalse();
    }
}
