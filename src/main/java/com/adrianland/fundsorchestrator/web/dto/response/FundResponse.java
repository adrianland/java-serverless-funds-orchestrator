package com.adrianland.fundsorchestrator.web.dto.response;

import com.adrianland.fundsorchestrator.domain.model.Fund;
import com.adrianland.fundsorchestrator.domain.model.enums.FundCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Investment fund details")
public record FundResponse(
        @Schema(example = "1") String fundId,
        @Schema(example = "FPV_BTG_PACTUAL_RECAUDADORA") String name,
        @Schema(example = "75000") BigDecimal minAmount,
        @Schema(example = "FPV") FundCategory category
) {
    public static FundResponse from(Fund fund) {
        return new FundResponse(fund.getFundId(), fund.getName(),
                fund.getMinAmount(), fund.getCategory());
    }
}
