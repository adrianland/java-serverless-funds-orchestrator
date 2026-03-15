package com.adrianland.fundsorchestrator.web.dto.response;

import com.adrianland.fundsorchestrator.domain.model.Transaction;
import com.adrianland.fundsorchestrator.domain.model.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Transaction record returned after a fund operation")
public record TransactionResponse(

        @Schema(description = "Unique transaction ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String txId,

        @Schema(description = "Client ID", example = "CLIENT-001")
        String clientId,

        @Schema(description = "Fund ID", example = "1")
        String fundId,

        @Schema(description = "Fund name", example = "FPV_BTG_PACTUAL_RECAUDADORA")
        String fundName,

        @Schema(description = "Transaction type", example = "APERTURA")
        TransactionType type,

        @Schema(description = "Amount in COP", example = "75000")
        BigDecimal amount,

        @Schema(description = "Timestamp (UTC)")
        Instant createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getTxId(), tx.getClientId(), tx.getFundId(),
                tx.getFundName(), tx.getType(), tx.getAmount(), tx.getCreatedAt()
        );
    }
}
