package com.adrianland.fundsorchestrator.infrastructure.dynamodb;

import com.adrianland.fundsorchestrator.domain.model.Fund;
import com.adrianland.fundsorchestrator.domain.model.enums.FundCategory;
import com.adrianland.fundsorchestrator.domain.port.FundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.*;

import static com.adrianland.fundsorchestrator.infrastructure.dynamodb.DynamoDbKeys.*;

@Repository
@RequiredArgsConstructor
public class DynamoDbFundRepository implements FundRepository {

    @Value("${aws.dynamodb.table-name:FondosBTG}")
    private String tableName;

    private final DynamoDbClient dynamoDbClient;

    @Override
    public Optional<Fund> findById(String fundId) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        ATTR_PK, AttributeValue.fromS(fundPk(fundId)),
                        ATTR_SK, AttributeValue.fromS(SK_METADATA)))
                .build());

        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapToFund(response.item()));
    }

    @Override
    public List<Fund> findAll() {
        // Scan for all items whose PK begins with "FUND#" and SK = "METADATA"
        var response = dynamoDbClient.scan(ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("begins_with(#pk, :prefix) AND #sk = :meta")
                .expressionAttributeNames(Map.of("#pk", ATTR_PK, "#sk", ATTR_SK))
                .expressionAttributeValues(Map.of(
                        ":prefix", AttributeValue.fromS(PREFIX_FUND),
                        ":meta",   AttributeValue.fromS(SK_METADATA)))
                .build());

        return response.items().stream()
                .map(this::mapToFund)
                .toList();
    }

    @Override
    public void save(Fund fund) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        ATTR_PK,         AttributeValue.fromS(fundPk(fund.getFundId())),
                        ATTR_SK,         AttributeValue.fromS(SK_METADATA),
                        ATTR_FUND_ID,    AttributeValue.fromS(fund.getFundId()),
                        ATTR_NAME,       AttributeValue.fromS(fund.getName()),
                        ATTR_MIN_AMOUNT, AttributeValue.fromN(fund.getMinAmount().toPlainString()),
                        ATTR_CATEGORY,   AttributeValue.fromS(fund.getCategory().name())
                ))
                .build());
    }

    private Fund mapToFund(Map<String, AttributeValue> item) {
        return Fund.builder()
                .fundId(item.get(ATTR_FUND_ID).s())
                .name(item.get(ATTR_NAME).s())
                .minAmount(new BigDecimal(item.get(ATTR_MIN_AMOUNT).n()))
                .category(FundCategory.valueOf(item.get(ATTR_CATEGORY).s()))
                .build();
    }
}
