package com.franchise.application.usecase;

import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

public class UpdateFranchiseNameUseCase {

    private final FranchiseRepository franchiseRepository;

    public UpdateFranchiseNameUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<Franchise> execute(UpdateFranchiseNameRequest request) {
        return Mono.fromCallable(() -> franchiseRepository.findById(request.franchiseId()))
                .flatMap(optionalFranchise -> {
                    Franchise franchise = optionalFranchise.orElseThrow(() -> new IllegalArgumentException("Franchise not found"));
                    Franchise updated = franchise.toBuilder()
                            .name(request.newName())
                            .build();
                    return Mono.fromCallable(() -> franchiseRepository.save(updated));
                });
    }

    public record UpdateFranchiseNameRequest(String franchiseId, String newName) {}
}