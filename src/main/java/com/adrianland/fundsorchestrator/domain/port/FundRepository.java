package com.adrianland.fundsorchestrator.domain.port;

import com.adrianland.fundsorchestrator.domain.model.Fund;

import java.util.List;
import java.util.Optional;

public interface FundRepository {

    Optional<Fund> findById(String fundId);

    List<Fund> findAll();

    void save(Fund fund);
}
