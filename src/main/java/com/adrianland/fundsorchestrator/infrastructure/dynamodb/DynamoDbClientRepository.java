package com.adrianland.fundsorchestrator.infrastructure.dynamodb;

import com.adrianland.fundsorchestrator.domain.model.Client;
import com.adrianland.fundsorchestrator.domain.model.enums.NotificationType;
import com.adrianland.fundsorchestrator.domain.port.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static com.adrianland.fundsorchestrator.infrastructure.dynamodb.DynamoDbKeys.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DynamoDbClientRepository implements ClientRepository {

    @Value("${aws.dynamodb.table-name:FondosBTG}")
    private String tableName;

    private final DynamoDbClient dynamoDbClient;

    @Override
    public Optional<Client> findById(String clientId) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        ATTR_PK, AttributeValue.fromS(clientPk(clientId)),
                        ATTR_SK, AttributeValue.fromS(SK_METADATA)))
                .build());

        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapToClient(response.item()));
    }

    @Override
    public void save(Client client) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        ATTR_PK,           AttributeValue.fromS(clientPk(client.getClientId())),
                        ATTR_SK,           AttributeValue.fromS(SK_METADATA),
                        ATTR_CLIENT_ID,    AttributeValue.fromS(client.getClientId()),
                        ATTR_NAME,         AttributeValue.fromS(client.getName()),
                        ATTR_EMAIL,        AttributeValue.fromS(client.getEmail()),
                        ATTR_PHONE,        AttributeValue.fromS(client.getPhone()),
                        ATTR_BALANCE,      AttributeValue.fromN(client.getBalance().toPlainString()),
                        ATTR_NOTIFICATION, AttributeValue.fromS(client.getNotificationPreference().name())
                ))
                .build());
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    private Client mapToClient(Map<String, AttributeValue> item) {
        return Client.builder()
                .clientId(item.get(ATTR_CLIENT_ID).s())
                .name(item.get(ATTR_NAME).s())
                .email(item.get(ATTR_EMAIL).s())
                .phone(item.get(ATTR_PHONE).s())
                .balance(new BigDecimal(item.get(ATTR_BALANCE).n()))
                .notificationPreference(
                        NotificationType.valueOf(item.get(ATTR_NOTIFICATION).s()))
                .build();
    }
}
