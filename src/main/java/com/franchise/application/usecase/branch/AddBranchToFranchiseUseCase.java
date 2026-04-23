package com.franchise.application.usecase.branch;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddBranchToFranchiseUseCase {

    private final FranchiseRepository franchiseRepository;

    public AddBranchToFranchiseUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<Franchise> execute(AddBranchToFranchiseRequest request) {
        return Mono.fromSupplier(() -> {
            Franchise franchise = franchiseRepository.findById(request.franchiseId())
                    .orElseThrow(() -> new IllegalArgumentException("Franchise not found"));

            List<Branch> updatedBranches = new ArrayList<>(franchise.getBranches());
            updatedBranches.add(Branch.builder()
                    .id(UUID.randomUUID().toString())
                    .name(request.branchName())
                    .products(List.of())
                    .build());

            return franchiseRepository.save(franchise.toBuilder().branches(updatedBranches).build());
        });
    }

    public record AddBranchToFranchiseRequest(String franchiseId, String branchName) {}
}
