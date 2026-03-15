package com.adrianland.fundsorchestrator.web.controller;

import com.adrianland.fundsorchestrator.application.usecase.*;
import com.adrianland.fundsorchestrator.web.dto.request.SubscribeFundRequest;
import com.adrianland.fundsorchestrator.web.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients/{clientId}")
@RequiredArgsConstructor
@Tag(name = "Funds", description = "Investment fund subscription management")
public class FundController {

    private final SubscribeFundUseCase subscribeFundUseCase;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
    private final GetTransactionHistoryUseCase getTransactionHistoryUseCase;
    private final ListFundsUseCase listFundsUseCase;
    private final GetClientUseCase getClientUseCase;

    // ── GET /clients/{clientId} ───────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get client details",
            description = "Returns client profile and current available balance.")
    @ApiResponse(responseCode = "200", description = "Client found")
    @ApiResponse(responseCode = "404", description = "Client not found")
    public ResponseEntity<ClientResponse> getClient(
            @PathVariable String clientId) {

        return ResponseEntity.ok(ClientResponse.from(getClientUseCase.execute(clientId)));
    }

    // ── GET /clients/{clientId}/funds ─────────────────────────────────────────

    @GetMapping("/funds")
    @Operation(summary = "List all available funds",
            description = "Returns the full catalog of BTG Pactual investment funds.")
    public ResponseEntity<List<FundResponse>> listFunds(
            @PathVariable String clientId) {

        return ResponseEntity.ok(
                listFundsUseCase.execute().stream().map(FundResponse::from).toList());
    }

    // ── POST /clients/{clientId}/funds ────────────────────────────────────────

    @PostMapping("/funds")
    @Operation(
            summary = "Subscribe to a fund (apertura)",
            description = """
            Subscribes the client to the specified investment fund.
            
            The `X-Idempotency-Key` header prevents double-charges on network retries.
            Supply the same UUID on retries; the original transaction is returned.
            """)
    @ApiResponse(responseCode = "201", description = "Subscription created")
    @ApiResponse(responseCode = "400", description = "Insufficient balance or already subscribed")
    @ApiResponse(responseCode = "404", description = "Client or fund not found")
    public ResponseEntity<TransactionResponse> subscribe(
            @PathVariable String clientId,
            @Valid @RequestBody SubscribeFundRequest request,
            @Parameter(description = "Unique key to ensure idempotency (UUID recommended)")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        var tx = subscribeFundUseCase.execute(clientId, request.fundId(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(tx));
    }

    // ── DELETE /clients/{clientId}/funds/{fundId} ─────────────────────────────

    @DeleteMapping("/funds/{fundId}")
    @Operation(
            summary = "Cancel fund subscription (cancelación)",
            description = "Cancels the subscription and credits the locked amount back to the client.")
    @ApiResponse(responseCode = "200", description = "Subscription cancelled")
    @ApiResponse(responseCode = "400", description = "Not subscribed to this fund")
    @ApiResponse(responseCode = "404", description = "Client or fund not found")
    public ResponseEntity<TransactionResponse> cancel(
            @PathVariable String clientId,
            @PathVariable String fundId) {

        var tx = cancelSubscriptionUseCase.execute(clientId, fundId);
        return ResponseEntity.ok(TransactionResponse.from(tx));
    }

    // ── GET /clients/{clientId}/transactions ──────────────────────────────────

    @GetMapping("/transactions")
    @Operation(
            summary = "Get transaction history",
            description = "Returns the full chronological list of fund operations (newest first).")
    @ApiResponse(responseCode = "200", description = "Transaction history returned")
    @ApiResponse(responseCode = "404", description = "Client not found")
    public ResponseEntity<List<TransactionResponse>> getHistory(
            @PathVariable String clientId) {

        return ResponseEntity.ok(
                getTransactionHistoryUseCase.execute(clientId).stream()
                        .map(TransactionResponse::from)
                        .toList());
    }
}
