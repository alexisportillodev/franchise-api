package com.franchise.application.usecase;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public class UpdateBranchNameUseCase {

    private final FranchiseRepository franchiseRepository;

    public UpdateBranchNameUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<Franchise> execute(UpdateBranchNameRequest request) {
        return Mono.fromCallable(() -> franchiseRepository.findById(request.franchiseId())
                .orElseThrow(() -> new IllegalArgumentException("Franchise not found")))
                .flatMap(franchise -> {
                    List<Branch> updatedBranches = franchise.getBranches().stream()
                            .map(branch -> branch.getId().equals(request.branchId())
                                    ? branch.toBuilder().name(request.newName()).build()
                                    : branch)
                            .collect(Collectors.toList());
                    Franchise updatedFranchise = franchise.toBuilder().branches(updatedBranches).build();
                    return Mono.fromCallable(() -> franchiseRepository.save(updatedFranchise));
                });
    }

    public record UpdateBranchNameRequest(String franchiseId, String branchId, String newName) {}
}