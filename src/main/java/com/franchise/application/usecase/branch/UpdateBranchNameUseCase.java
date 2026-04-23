package com.franchise.application.usecase.branch;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.List;

public class UpdateBranchNameUseCase {

    private final FranchiseRepository franchiseRepository;
    private final FindBranchLocationUseCase findBranchLocationUseCase;

    public UpdateBranchNameUseCase(FranchiseRepository franchiseRepository,
                                   FindBranchLocationUseCase findBranchLocationUseCase) {
        this.franchiseRepository = franchiseRepository;
        this.findBranchLocationUseCase = findBranchLocationUseCase;
    }

    public Mono<Franchise> execute(UpdateBranchNameRequest request) {
        return findBranchLocationUseCase.execute(new FindBranchLocationUseCase.FindBranchLocationRequest(request.branchId()))
                .map(location -> updateBranchName(location.franchiseId(), request));
    }

    private Franchise updateBranchName(String franchiseId, UpdateBranchNameRequest request) {
        Franchise franchise = franchiseRepository.findById(franchiseId)
                .orElseThrow(() -> new IllegalArgumentException("Franchise not found"));

        List<Branch> updatedBranches = franchise.getBranches().stream()
                .map(branch -> branch.getId().equals(request.branchId())
                        ? branch.toBuilder().name(request.newName()).build()
                        : branch)
                .toList();

        return franchiseRepository.save(franchise.toBuilder().branches(updatedBranches).build());
    }

    public record UpdateBranchNameRequest(String branchId, String newName) {}
}
