package com.adrianland.fundsorchestrator.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for subscribing a client to an investment fund")
public record SubscribeFundRequest(

        @NotBlank(message = "fundId is required")
        @Schema(description = "Target fund ID", example = "1")
        String fundId
) {}
