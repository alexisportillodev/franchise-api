package com.franchise.application.usecase.franchise;

import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

public class UpdateFranchiseNameUseCase {

    private final FranchiseRepository franchiseRepository;

    public UpdateFranchiseNameUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<Franchise> execute(UpdateFranchiseNameRequest request) {
        return Mono.fromSupplier(() -> franchiseRepository.findById(request.franchiseId())
                .map(franchise -> franchise.toBuilder().name(request.newName()).build())
                .map(franchiseRepository::save)
                .orElseThrow(() -> new IllegalArgumentException("Franchise not found")));
    }

    public record UpdateFranchiseNameRequest(String franchiseId, String newName) {}
}
