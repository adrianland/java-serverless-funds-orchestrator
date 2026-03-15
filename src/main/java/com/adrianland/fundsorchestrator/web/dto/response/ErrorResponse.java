package com.adrianland.fundsorchestrator.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standard error response")
public record ErrorResponse(
        @Schema(example = "400") int status,
        @Schema(example = "INSUFFICIENT_BALANCE") String error,
        @Schema(example = "No tiene saldo disponible para vincularse al fondo FPV_BTG_PACTUAL_RECAUDADORA")
        String message,
        Instant timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, Instant.now());
    }
}
