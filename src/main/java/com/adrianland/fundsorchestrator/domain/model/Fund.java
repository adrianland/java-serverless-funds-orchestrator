package com.adrianland.fundsorchestrator.domain.model;

import com.adrianland.fundsorchestrator.domain.model.enums.FundCategory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Represents a BTG Pactual investment fund.
 *
 * <p>Seed data (from the PDF spec):
 * <pre>
 *  1 · FPV_BTG_PACTUAL_RECAUDADORA  COP   75 000  FPV
 *  2 · FPV_BTG_PACTUAL_ECOPETROL    COP  125 000  FPV
 *  3 · DEUDAPRIVADA                 COP   50 000  FIC
 *  4 · FDO-ACCIONES                 COP  250 000  FIC
 *  5 · FPV_BTG_PACTUAL_DINAMICA     COP  100 000  FPV
 * </pre>
 * </p>
 */
@Getter
@Builder
public class Fund {

    private final String fundId;
    private final String name;

    /** Minimum subscription amount in COP. */
    private final BigDecimal minAmount;

    private final FundCategory category;
}
