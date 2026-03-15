package com.adrianland.fundsorchestrator.domain.port;

import java.util.Optional;

public interface IdempotencyRepository {

    Optional<String> findTxIdByKey(String idempotencyKey);
}
