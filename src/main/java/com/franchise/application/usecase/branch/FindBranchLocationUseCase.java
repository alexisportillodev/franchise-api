package com.franchise.application.usecase.branch;

import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

public class FindBranchLocationUseCase {

    private final FranchiseRepository franchiseRepository;

    public FindBranchLocationUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<BranchLocation> execute(FindBranchLocationRequest request) {
        return Mono.fromSupplier(() -> franchiseRepository.findAll().stream()
                .filter(franchise -> containsBranch(franchise, request.branchId()))
                .findFirst()
                .map(franchise -> new BranchLocation(franchise.getId(), request.branchId()))
                .orElseThrow(() -> new IllegalArgumentException("Branch not found")));
    }

    private boolean containsBranch(Franchise franchise, String branchId) {
        return franchise.getBranches().stream().anyMatch(branch -> branch.getId().equals(branchId));
    }

    public record FindBranchLocationRequest(String branchId) {}

    public record BranchLocation(String franchiseId, String branchId) {}
}
