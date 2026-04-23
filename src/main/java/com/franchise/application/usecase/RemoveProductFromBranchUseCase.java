package com.franchise.application.usecase;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public class RemoveProductFromBranchUseCase {

    private final FranchiseRepository franchiseRepository;

    public RemoveProductFromBranchUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<Franchise> execute(RemoveProductFromBranchRequest request) {
        return Mono.fromCallable(() -> franchiseRepository.findById(request.franchiseId())
                .orElseThrow(() -> new IllegalArgumentException("Franchise not found")))
                .flatMap(franchise -> {
                    List<Branch> updatedBranches = franchise.getBranches().stream()
                            .map(branch -> branch.getId().equals(request.branchId())
                                    ? removeProductFromBranch(branch, request.productId())
                                    : branch)
                            .collect(Collectors.toList());
                    Franchise updatedFranchise = franchise.toBuilder().branches(updatedBranches).build();
                    return Mono.fromCallable(() -> franchiseRepository.save(updatedFranchise));
                });
    }

    private Branch removeProductFromBranch(Branch branch, String productId) {
        List<com.franchise.domain.model.Product> updatedProducts = branch.getProducts().stream()
                .filter(product -> !product.getId().equals(productId))
                .collect(Collectors.toList());
        return branch.toBuilder().products(updatedProducts).build();
    }

    public record RemoveProductFromBranchRequest(String franchiseId, String branchId, String productId) {}
}