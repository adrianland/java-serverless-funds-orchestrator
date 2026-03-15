package com.adrianland.fundsorchestrator.infrastructure.dynamodb;

import com.adrianland.fundsorchestrator.domain.port.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import java.util.Map;
import java.util.Optional;

import static com.adrianland.fundsorchestrator.infrastructure.dynamodb.DynamoDbKeys.*;

@Repository
@RequiredArgsConstructor
public class DynamoDbIdempotencyRepository implements IdempotencyRepository {

    @Value("${aws.dynamodb.table-name:FondosBTG}")
    private String tableName;

    private final DynamoDbClient dynamoDbClient;

    @Override
    public Optional<String> findTxIdByKey(String idempotencyKey) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        ATTR_PK, AttributeValue.fromS(idempotencyPk(idempotencyKey)),
                        ATTR_SK, AttributeValue.fromS(SK_METADATA)))
                .build());

        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(response.item().get(ATTR_TX_ID))
                .map(AttributeValue::s);
    }
}
