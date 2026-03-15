package com.adrianland.fundsorchestrator.infrastructure.dynamodb;

import com.adrianland.fundsorchestrator.domain.model.Transaction;
import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;
import com.adrianland.fundsorchestrator.domain.port.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static com.adrianland.fundsorchestrator.infrastructure.dynamodb.DynamoDbKeys.*;

@Repository
@RequiredArgsConstructor
public class DynamoDbTransactionRepository implements TransactionRepository {

    @Value("${aws.dynamodb.table-name:FondosBTG}")
    private String tableName;

    private final DynamoDbClient dynamoDbClient;

    @Override
    public void save(Transaction tx) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(buildItem(tx))
                .build());
    }

    @Override
    public List<Transaction> findByClientId(String clientId) {
        // Query with PK = CLIENT#<id> and SK begins_with "TX#"
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("#pk = :pk AND begins_with(#sk, :prefix)")
                .expressionAttributeNames(Map.of("#pk", ATTR_PK, "#sk", ATTR_SK))
                .expressionAttributeValues(Map.of(
                        ":pk",     AttributeValue.fromS(clientPk(clientId)),
                        ":prefix", AttributeValue.fromS(PREFIX_TX)))
                .scanIndexForward(false) // newest first
                .build());

        return response.items().stream()
                .map(this::mapToTransaction)
                .toList();
    }

    // ── Package-visible so SubscriptionRepository can reuse it ──────────────

    Map<String, AttributeValue> buildItem(Transaction tx) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(ATTR_PK,        AttributeValue.fromS(clientPk(tx.getClientId())));
        item.put(ATTR_SK,        AttributeValue.fromS(
                transactionSk(tx.getCreatedAt().toString(), tx.getTxId())));
        item.put(ATTR_TX_ID,     AttributeValue.fromS(tx.getTxId()));
        item.put(ATTR_CLIENT_ID, AttributeValue.fromS(tx.getClientId()));
        item.put(ATTR_FUND_ID,   AttributeValue.fromS(tx.getFundId()));
        item.put(ATTR_FUND_NAME, AttributeValue.fromS(tx.getFundName()));
        item.put(ATTR_TX_TYPE,   AttributeValue.fromS(tx.getType().name()));
        item.put(ATTR_AMOUNT,    AttributeValue.fromN(tx.getAmount().toPlainString()));
        item.put(ATTR_CREATED_AT,AttributeValue.fromS(tx.getCreatedAt().toString()));
        if (tx.getIdempotencyKey() != null) {
            item.put(ATTR_IDEMPOTENCY_KEY, AttributeValue.fromS(tx.getIdempotencyKey()));
        }
        return item;
    }

    private Transaction mapToTransaction(Map<String, AttributeValue> item) {
        return Transaction.builder()
                .txId(item.get(ATTR_TX_ID).s())
                .clientId(item.get(ATTR_CLIENT_ID).s())
                .fundId(item.get(ATTR_FUND_ID).s())
                .fundName(item.get(ATTR_FUND_NAME).s())
                .type(TransactionType.valueOf(item.get(ATTR_TX_TYPE).s()))
                .amount(new BigDecimal(item.get(ATTR_AMOUNT).n()))
                .createdAt(Instant.parse(item.get(ATTR_CREATED_AT).s()))
                .idempotencyKey(item.containsKey(ATTR_IDEMPOTENCY_KEY)
                        ? item.get(ATTR_IDEMPOTENCY_KEY).s() : null)
                .build();
    }
}
