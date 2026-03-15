package com.adrianland.fundsorchestrator.application.usecase;

import com.adrianland.fundsorchestrator.domain.model.Fund;

import java.util.List;

/** List all available BTG Pactual investment funds. */
public interface ListFundsUseCase {

    List<Fund> execute();
}
