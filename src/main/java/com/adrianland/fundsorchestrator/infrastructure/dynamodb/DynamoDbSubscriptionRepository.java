package com.adrianland.fundsorchestrator.infrastructure.dynamodb;

import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.Subscription;
import com.adrianland.fundsorchestrator.domain.model.Transaction;
import com.adrianland.fundsorchestrator.domain.port.SubscriptionRepository;
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
public class DynamoDbSubscriptionRepository implements SubscriptionRepository {

    @Value("${aws.dynamodb.table-name:FondosBTG}")
    private String tableName;

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbTransactionRepository txHelper;

    @Override
    public Optional<Subscription> findByClientAndFund(String clientId, String fundId) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        ATTR_PK, AttributeValue.fromS(clientPk(clientId)),
                        ATTR_SK, AttributeValue.fromS(subscriptionSk(fundId))))
                .build());

        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapToSubscription(response.item()));
    }

    @Override
    public List<Subscription> findByClientId(String clientId) {
        var response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("#pk = :pk AND begins_with(#sk, :prefix)")
                .expressionAttributeNames(Map.of("#pk", ATTR_PK, "#sk", ATTR_SK))
                .expressionAttributeValues(Map.of(
                        ":pk",     AttributeValue.fromS(clientPk(clientId)),
                        ":prefix", AttributeValue.fromS(PREFIX_FUND)))
                .build());

        return response.items().stream().map(this::mapToSubscription).toList();
    }

    @Override
    public void createSubscriptionAtomically(Subscription subscription,
                                             Transaction transaction,
                                             Client updatedClient,
                                             String idempotencyKey) {
        List<TransactWriteItem> writes = new ArrayList<>();

        // 1. Persist subscription item
        writes.add(TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(tableName)
                        .item(buildSubscriptionItem(subscription))
                        .conditionExpression("attribute_not_exists(#pk)")
                        .expressionAttributeNames(Map.of("#pk", ATTR_PK))
                        .build())
                .build());

        // 2. Persist transaction record
        writes.add(TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(tableName)
                        .item(txHelper.buildItem(transaction))
                        .build())
                .build());

        // 3. Update client balance
        writes.add(TransactWriteItem.builder()
                .update(Update.builder()
                        .tableName(tableName)
                        .key(Map.of(
                                ATTR_PK, AttributeValue.fromS(clientPk(updatedClient.getClientId())),
                                ATTR_SK, AttributeValue.fromS(SK_METADATA)))
                        .updateExpression("SET #bal = :bal")
                        .expressionAttributeNames(Map.of("#bal", ATTR_BALANCE))
                        .expressionAttributeValues(Map.of(
                                ":bal", AttributeValue.fromN(
                                        updatedClient.getBalance().toPlainString())))
                        .build())
                .build());

        // 4. Record idempotency key (with 24 h TTL via conditional put)
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            writes.add(TransactWriteItem.builder()
                    .put(Put.builder()
                            .tableName(tableName)
                            .item(Map.of(
                                    ATTR_PK,    AttributeValue.fromS(idempotencyPk(idempotencyKey)),
                                    ATTR_SK,    AttributeValue.fromS(SK_METADATA),
                                    ATTR_TX_ID, AttributeValue.fromS(transaction.getTxId())))
                            .conditionExpression("attribute_not_exists(#pk)")
                            .expressionAttributeNames(Map.of("#pk", ATTR_PK))
                            .build())
                    .build());
        }

        dynamoDbClient.transactWriteItems(
                TransactWriteItemsRequest.builder().transactItems(writes).build());
    }

    @Override
    public void cancelSubscriptionAtomically(String clientId,
                                             String fundId,
                                             Transaction transaction,
                                             Client updatedClient) {
        List<TransactWriteItem> writes = List.of(
                // 1. Delete subscription
                TransactWriteItem.builder()
                        .delete(Delete.builder()
                                .tableName(tableName)
                                .key(Map.of(
                                        ATTR_PK, AttributeValue.fromS(clientPk(clientId)),
                                        ATTR_SK, AttributeValue.fromS(subscriptionSk(fundId))))
                                .build())
                        .build(),
                // 2. Persist cancellation transaction
                TransactWriteItem.builder()
                        .put(Put.builder()
                                .tableName(tableName)
                                .item(txHelper.buildItem(transaction))
                                .build())
                        .build(),
                // 3. Credit client balance
                TransactWriteItem.builder()
                        .update(Update.builder()
                                .tableName(tableName)
                                .key(Map.of(
                                        ATTR_PK, AttributeValue.fromS(clientPk(clientId)),
                                        ATTR_SK, AttributeValue.fromS(SK_METADATA)))
                                .updateExpression("SET #bal = :bal")
                                .expressionAttributeNames(Map.of("#bal", ATTR_BALANCE))
                                .expressionAttributeValues(Map.of(
                                        ":bal", AttributeValue.fromN(
                                                updatedClient.getBalance().toPlainString())))
                                .build())
                        .build()
        );

        dynamoDbClient.transactWriteItems(
                TransactWriteItemsRequest.builder().transactItems(writes).build());
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private Map<String, AttributeValue> buildSubscriptionItem(Subscription s) {
        return Map.of(
                ATTR_PK,           AttributeValue.fromS(clientPk(s.getClientId())),
                ATTR_SK,           AttributeValue.fromS(subscriptionSk(s.getFundId())),
                ATTR_CLIENT_ID,    AttributeValue.fromS(s.getClientId()),
                ATTR_FUND_ID,      AttributeValue.fromS(s.getFundId()),
                ATTR_FUND_NAME,    AttributeValue.fromS(s.getFundName()),
                ATTR_AMOUNT,       AttributeValue.fromN(s.getAmount().toPlainString()),
                ATTR_SUBSCRIBED_AT,AttributeValue.fromS(s.getSubscribedAt().toString())
        );
    }

    private Subscription mapToSubscription(Map<String, AttributeValue> item) {
        return Subscription.builder()
                .clientId(item.get(ATTR_CLIENT_ID).s())
                .fundId(item.get(ATTR_FUND_ID).s())
                .fundName(item.get(ATTR_FUND_NAME).s())
                .amount(new BigDecimal(item.get(ATTR_AMOUNT).n()))
                .subscribedAt(Instant.parse(item.get(ATTR_SUBSCRIBED_AT).s()))
                .build();
    }
}
