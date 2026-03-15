package com.adrianland.fundsorchestrator.infrastructure.dynamodb;

import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.Fund;
import com.adrianland.fundsorchestrator.domain.model.enums.FundCategory;
import com.adrianland.fundsorchestrator.domain.model.enums.NotificationType;
import com.adrianland.fundsorchestrator.domain.port.ClientRepository;
import com.adrianland.fundsorchestrator.domain.port.FundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamoDbTableInitializer implements ApplicationRunner {

    /** Only retry transient 5xx errors from DynamoDB Local warm-up. */
    private static final int  MAX_RETRIES = 5;
    private static final long BACKOFF_MS  = 1_000L;

    @Value("${aws.dynamodb.table-name:FondosBTG}")
    private String tableName;

    @Value("${aws.dynamodb.endpoint:}")
    private String endpoint;

    private final DynamoDbClient dynamoDbClient;
    private final FundRepository fundRepository;
    private final ClientRepository clientRepository;

    @Override
    public void run(ApplicationArguments args) {
        createTableIfNotExists();
        seedFunds();
        seedDemoClient();
    }

    // ── Table bootstrap ──────────────────────────────────────────────────────

    private void createTableIfNotExists() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (tableExists()) {
                    log.info("event=table_already_exists table={}", tableName);
                } else {
                    doCreateTable();
                }
                return; // success — exit retry loop

            } catch (ResourceNotFoundException e) {
                // Table does not exist yet — create it (should not reach here,
                // tableExists() already handles this, but kept as safety net)
                doCreateTable();
                return;

            } catch (DynamoDbException ex) {
                int statusCode = ex.statusCode();

                // ── 4xx = configuration error → fail fast, no retry ──────────
                // Common causes:
                //   400 "security token invalid"  → DYNAMODB_ENDPOINT not set,
                //                                   app is hitting real AWS with
                //                                   dummy credentials (local/local).
                //   403 "missing authentication"  → same root cause.
                // Retrying will never fix a missing/wrong endpoint configuration.
                if (statusCode >= 400 && statusCode < 500) {
                    String hint = endpoint == null || endpoint.isBlank()
                            ? "DYNAMODB_ENDPOINT is not set — the app is trying to reach real AWS. "
                            + "Set DYNAMODB_ENDPOINT=http://localhost:8000 in your IntelliJ "
                            + "run configuration (Run → Edit Configurations → Environment variables)."
                            : "DynamoDB returned HTTP " + statusCode + ". "
                            + "Check your endpoint (" + endpoint + ") and credentials.";

                    throw new IllegalStateException(
                            "DynamoDB configuration error (HTTP " + statusCode + "): "
                                    + ex.getMessage() + " — " + hint, ex);
                }

                // ── 5xx = transient DynamoDB Local warm-up error → retry ─────
                log.warn("event=dynamo_init_retry attempt={}/{} statusCode={} message={}",
                        attempt, MAX_RETRIES, statusCode, ex.getMessage());

                if (attempt == MAX_RETRIES) {
                    throw new IllegalStateException(
                            "DynamoDB Local did not become ready after " + MAX_RETRIES
                                    + " attempts (last HTTP status: " + statusCode + "). "
                                    + "Make sure the dynamodb-local container is running and healthy.", ex);
                }
                sleep(BACKOFF_MS * attempt);
            }
        }
    }

    private boolean tableExists() {
        try {
            dynamoDbClient.describeTable(r -> r.tableName(tableName));
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    private void doCreateTable() {
        log.info("event=creating_table table={}", tableName);
        dynamoDbClient.createTable(CreateTableRequest.builder()
                .tableName(tableName)
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName(DynamoDbKeys.ATTR_PK)
                                .attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder()
                                .attributeName(DynamoDbKeys.ATTR_SK)
                                .attributeType(ScalarAttributeType.S).build())
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName(DynamoDbKeys.ATTR_PK)
                                .keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder()
                                .attributeName(DynamoDbKeys.ATTR_SK)
                                .keyType(KeyType.RANGE).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());

        dynamoDbClient.waiter()
                .waitUntilTableExists(r -> r.tableName(tableName));
        log.info("event=table_created table={}", tableName);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Seed data ────────────────────────────────────────────────────────────

    private void seedFunds() {
        List<Fund> funds = List.of(
                Fund.builder().fundId("1").name("FPV_BTG_PACTUAL_RECAUDADORA")
                        .minAmount(new BigDecimal("75000")).category(FundCategory.FPV).build(),
                Fund.builder().fundId("2").name("FPV_BTG_PACTUAL_ECOPETROL")
                        .minAmount(new BigDecimal("125000")).category(FundCategory.FPV).build(),
                Fund.builder().fundId("3").name("DEUDAPRIVADA")
                        .minAmount(new BigDecimal("50000")).category(FundCategory.FIC).build(),
                Fund.builder().fundId("4").name("FDO-ACCIONES")
                        .minAmount(new BigDecimal("250000")).category(FundCategory.FIC).build(),
                Fund.builder().fundId("5").name("FPV_BTG_PACTUAL_DINAMICA")
                        .minAmount(new BigDecimal("100000")).category(FundCategory.FPV).build()
        );

        funds.forEach(fund -> {
            if (fundRepository.findById(fund.getFundId()).isEmpty()) {
                fundRepository.save(fund);
                log.info("event=fund_seeded fundId={} name={}", fund.getFundId(), fund.getName());
            }
        });
    }

    private void seedDemoClient() {
        String demoClientId = "CLIENT-001";
        if (clientRepository.findById(demoClientId).isEmpty()) {
            Client demo = Client.builder()
                    .clientId(demoClientId)
                    .name("Demo Client BTG")
                    .email("adrian@btgpactual.com")
                    .phone("+573001234567")
                    .balance(new BigDecimal("500000"))
                    .notificationPreference(NotificationType.EMAIL)
                    .build();
            clientRepository.save(demo);
            log.info("event=demo_client_seeded clientId={} balance=500000", demoClientId);
        }
    }
}