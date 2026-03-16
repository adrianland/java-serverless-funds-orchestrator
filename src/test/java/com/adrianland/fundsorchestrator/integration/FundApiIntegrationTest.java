package com.adrianland.fundsorchestrator.integration;

import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.enums.NotificationType;
import com.adrianland.fundsorchestrator.web.dto.request.SubscribeFundRequest;
import com.adrianland.fundsorchestrator.web.dto.response.TransactionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

/**
 * Full integration test: Spring context + LocalStack DynamoDB in Docker.
 *
 * <p>Spins up a real LocalStack container using Testcontainers, exercises the
 * entire stack (controller → service → DynamoDB) via MockMvc HTTP calls.</p>
 *
 * <p>Run with: {@code mvn verify}</p>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Fund API – Integration Tests (LocalStack DynamoDB)")
class FundApiIntegrationTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.3"))
            .withServices(DYNAMODB);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.dynamodb.endpoint",
                () -> localStack.getEndpointOverride(DYNAMODB).toString());
        registry.add("aws.dynamodb.region",      localStack::getRegion);
        registry.add("aws.dynamodb.access-key",  localStack::getAccessKey);
        registry.add("aws.dynamodb.secret-key",  localStack::getSecretKey);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String CLIENT_ID   = "CLIENT-001";
    private static final String FUND_ID     = "1";
    private static final String BASE_PATH   = "/api/v1/clients/" + CLIENT_ID;

    // ── 1. List available funds ───────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("GET /funds should return 5 seeded BTG Pactual funds")
    void shouldListFunds() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/funds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].name").exists());
    }

    // ── 2. Get client ─────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("GET /clients/{id} should return demo client with 500 000 COP balance")
    void shouldGetClient() throws Exception {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(CLIENT_ID))
                .andExpect(jsonPath("$.balance").value(500000));
    }

    // ── 3. Subscribe to fund ──────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("POST /funds should create APERTURA transaction and return 201")
    void shouldSubscribeToFund() throws Exception {
        var request = new SubscribeFundRequest(FUND_ID);
        String idempotencyKey = UUID.randomUUID().toString();

        MvcResult result = mockMvc.perform(post(BASE_PATH + "/funds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.txId").exists())
                .andExpect(jsonPath("$.type").value("APERTURA"))
                .andExpect(jsonPath("$.amount").value(75000))
                .andReturn();

        TransactionResponse tx = objectMapper.readValue(
                result.getResponse().getContentAsString(), TransactionResponse.class);
        assertThat(tx.txId()).isNotBlank();
    }

    // ── 4. Idempotency: duplicate key returns same transaction ────────────────

    @Test
    @Order(4)
    @DisplayName("POST /funds with same idempotency key should return the original transaction")
    void shouldReturnSameTxOnDuplicateIdempotencyKey() throws Exception {
        String idempotencyKey = "fixed-idem-key-fund-2";
        var request = new SubscribeFundRequest("3"); // DEUDAPRIVADA – 50 000

        // First call
        MvcResult first = mockMvc.perform(post(BASE_PATH + "/funds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TransactionResponse tx1 = objectMapper.readValue(
                first.getResponse().getContentAsString(), TransactionResponse.class);

        // Second call – same key
        MvcResult second = mockMvc.perform(post(BASE_PATH + "/funds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        // Both calls return the same txId
        TransactionResponse tx2 = objectMapper.readValue(
                second.getResponse().getContentAsString(), TransactionResponse.class);
        assertThat(tx1.txId()).isEqualTo(tx2.txId());
    }

    // ── 5. Transaction history ────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("GET /transactions should return non-empty history after subscriptions")
    void shouldGetTransactionHistory() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    // ── 6. Insufficient balance ───────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("POST /funds should return 400 when balance is insufficient")
    void shouldReturn400WhenInsufficientBalance() throws Exception {
        // FDO-ACCIONES costs 250 000; client now has ~375 000 → subscribe twice more to deplete
        // Subscribe to ECOPETROL (125 000) first
        mockMvc.perform(post(BASE_PATH + "/funds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscribeFundRequest("2"))))
                .andExpect(status().isCreated());

        // Now try FDO-ACCIONES (250 000) – balance should be 200 000 → 400
        mockMvc.perform(post(BASE_PATH + "/funds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscribeFundRequest("4"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("FDO-ACCIONES")));
    }

    // ── 7. Cancel subscription ────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("DELETE /funds/{fundId} should cancel and return CANCELACION transaction")
    void shouldCancelSubscription() throws Exception {
        mockMvc.perform(delete(BASE_PATH + "/funds/" + FUND_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CANCELACION"))
                .andExpect(jsonPath("$.amount").value(75000));
    }

    // ── 8. Cancel non-existent subscription ──────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("DELETE /funds/{fundId} should return 400 when not subscribed")
    void shouldReturn400WhenNotSubscribed() throws Exception {
        // FUND 1 was cancelled in test 7
        mockMvc.perform(delete(BASE_PATH + "/funds/" + FUND_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("NOT_SUBSCRIBED"));
    }
}
