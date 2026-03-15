package com.adrianland.fundsorchestrator.application.service;

import com.adrianland.fundsorchestrator.application.usecase.ListFundsUseCase;
import com.adrianland.fundsorchestrator.domain.model.Fund;
import com.adrianland.fundsorchestrator.domain.port.FundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListFundsService implements ListFundsUseCase {

    private final FundRepository fundRepository;

    @Override
    public List<Fund> execute() {
        return fundRepository.findAll();
    }
}
