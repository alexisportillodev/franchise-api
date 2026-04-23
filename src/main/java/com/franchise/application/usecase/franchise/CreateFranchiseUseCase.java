package com.franchise.application.usecase.franchise;

import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public class CreateFranchiseUseCase {

    private final FranchiseRepository franchiseRepository;

    public CreateFranchiseUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<Franchise> execute(CreateFranchiseRequest request) {
        return Mono.fromSupplier(() -> franchiseRepository.save(
                Franchise.builder()
                        .id(UUID.randomUUID().toString())
                        .name(request.name())
                        .branches(List.of())
                        .build()
        ));
    }

    public record CreateFranchiseRequest(String name) {}
}
