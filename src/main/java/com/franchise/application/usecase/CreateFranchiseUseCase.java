package com.franchise.application.usecase;

import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.List;

public class CreateFranchiseUseCase {

    private final FranchiseRepository franchiseRepository;

    public CreateFranchiseUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<Franchise> execute(CreateFranchiseRequest request) {
        Franchise franchise = Franchise.builder()
                .id(generateId())
                .name(request.name())
                .branches(List.of())
                .build();
        return Mono.fromCallable(() -> franchiseRepository.save(franchise));
    }

    private String generateId() {
        return java.util.UUID.randomUUID().toString();
    }

    public record CreateFranchiseRequest(String name) {}
}