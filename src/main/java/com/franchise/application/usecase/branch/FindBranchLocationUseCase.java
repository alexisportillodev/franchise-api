package com.franchise.application.usecase.branch;

import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

public class FindBranchLocationUseCase {

    private final FranchiseRepository franchiseRepository;

    public FindBranchLocationUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<BranchLocation> execute(FindBranchLocationRequest request) {
        return Mono.fromSupplier(() -> franchiseRepository.findBranchLocation(request.branchId())
                .map(location -> new BranchLocation(location.franchiseId(), location.branchId()))
                .orElseThrow(() -> new IllegalArgumentException("Branch not found")));
    }

    public record FindBranchLocationRequest(String branchId) {}

    public record BranchLocation(String franchiseId, String branchId) {}
}
