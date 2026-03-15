package com.adrianland.fundsorchestrator.infrastructure.dynamodb;

/**
 * Single-Table Design constants for the {@code FondosBTG} DynamoDB table.
 *
 * <pre>
 * ┌───────────────────────────────┬──────────────────────────────────────────┐
 * │ Entity      │ PK              │ SK                                        │
 * ├─────────────┼─────────────────┼───────────────────────────────────────────┤
 * │ Client      │ CLIENT#<id>     │ METADATA                                  │
 * │ Subscription│ CLIENT#<id>     │ FUND#<fundId>                             │
 * │ Transaction │ CLIENT#<id>     │ TX#<ISO-8601>#<txId>                      │
 * │ Fund        │ FUND#<id>       │ METADATA                                  │
 * │ Idempotency │ IDEMPOTENCY#<k> │ METADATA                                  │
 * └─────────────┴─────────────────┴───────────────────────────────────────────┘
 * </pre>
 *
 * <p>All entities live in the same table. Queries for a client's full history
 * (subscriptions + transactions) use a single PK scan with SK begins_with.</p>
 */
public final class DynamoDbKeys {

    private DynamoDbKeys() {}

    // ── Key prefixes ────────────────────────────────────────────────────────
    public static final String PREFIX_CLIENT      = "CLIENT#";
    public static final String PREFIX_FUND        = "FUND#";
    public static final String PREFIX_TX          = "TX#";
    public static final String PREFIX_IDEMPOTENCY = "IDEMPOTENCY#";

    // ── Sort key literals ───────────────────────────────────────────────────
    public static final String SK_METADATA = "METADATA";

    // ── Attribute names ─────────────────────────────────────────────────────
    public static final String ATTR_PK               = "PK";
    public static final String ATTR_SK               = "SK";
    public static final String ATTR_CLIENT_ID        = "clientId";
    public static final String ATTR_NAME             = "name";
    public static final String ATTR_EMAIL            = "email";
    public static final String ATTR_PHONE            = "phone";
    public static final String ATTR_BALANCE          = "balance";
    public static final String ATTR_NOTIFICATION     = "notificationPreference";
    public static final String ATTR_FUND_ID          = "fundId";
    public static final String ATTR_FUND_NAME        = "fundName";
    public static final String ATTR_MIN_AMOUNT       = "minAmount";
    public static final String ATTR_CATEGORY         = "category";
    public static final String ATTR_AMOUNT           = "amount";
    public static final String ATTR_SUBSCRIBED_AT    = "subscribedAt";
    public static final String ATTR_TX_ID            = "txId";
    public static final String ATTR_TX_TYPE          = "type";
    public static final String ATTR_CREATED_AT       = "createdAt";
    public static final String ATTR_IDEMPOTENCY_KEY  = "idempotencyKey";

    // ── Key builders ─────────────────────────────────────────────────────────
    public static String clientPk(String clientId)        { return PREFIX_CLIENT + clientId; }
    public static String fundPk(String fundId)            { return PREFIX_FUND   + fundId; }
    public static String subscriptionSk(String fundId)    { return PREFIX_FUND   + fundId; }
    public static String transactionSk(String iso, String txId) {
        return PREFIX_TX + iso + "#" + txId;
    }
    public static String idempotencyPk(String key)        { return PREFIX_IDEMPOTENCY + key; }
}
