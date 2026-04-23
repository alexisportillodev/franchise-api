package com.franchise.application.usecase;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.BranchRepository;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class AddBranchToFranchiseUseCase {

    private final FranchiseRepository franchiseRepository;
    private final BranchRepository branchRepository;

    public AddBranchToFranchiseUseCase(FranchiseRepository franchiseRepository, BranchRepository branchRepository) {
        this.franchiseRepository = franchiseRepository;
        this.branchRepository = branchRepository;
    }

    public Mono<Franchise> execute(AddBranchToFranchiseRequest request) {
        return Mono.fromCallable(() -> franchiseRepository.findById(request.franchiseId()))
                .flatMap(optionalFranchise -> {
                    Franchise franchise = optionalFranchise.orElseThrow(() -> new IllegalArgumentException("Franchise not found"));
                    Branch newBranch = Branch.builder()
                            .id(generateId())
                            .name(request.branchName())
                            .products(List.of())
                            .build();
                    List<Branch> updatedBranches = new ArrayList<>(franchise.getBranches());
                    updatedBranches.add(newBranch);
                    Franchise updatedFranchise = franchise.toBuilder()
                            .branches(updatedBranches)
                            .build();
                    return Mono.fromCallable(() -> franchiseRepository.save(updatedFranchise));
                });
    }

    private String generateId() {
        return java.util.UUID.randomUUID().toString();
    }

    public record AddBranchToFranchiseRequest(String franchiseId, String branchName) {}
}