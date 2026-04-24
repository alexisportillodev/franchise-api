package com.franchise.application.usecase.product;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.List;

public class RemoveProductFromBranchUseCase {

    private final FranchiseRepository franchiseRepository;
    private final FindProductLocationUseCase findProductLocationUseCase;

    public RemoveProductFromBranchUseCase(FranchiseRepository franchiseRepository,
                                          FindProductLocationUseCase findProductLocationUseCase) {
        this.franchiseRepository = franchiseRepository;
        this.findProductLocationUseCase = findProductLocationUseCase;
    }

    public Mono<Franchise> execute(RemoveProductFromBranchRequest request) {
        return findProductLocationUseCase.execute(new FindProductLocationUseCase.FindProductLocationRequest(request.productId()))
                .map(location -> removeProduct(location.franchiseId(), location.branchId(), request.productId()));
    }

    private Franchise removeProduct(String franchiseId, String branchId, String productId) {
        Franchise franchise = franchiseRepository.findById(franchiseId)
                .orElseThrow(() -> new IllegalArgumentException("Franchise not found"));

        List<Branch> updatedBranches = franchise.getBranches().stream()
                .map(branch -> branch.getId().equals(branchId)
                        ? branch.toBuilder()
                                .products(branch.getProducts().stream()
                                        .filter(product -> !product.getId().equals(productId))
                                        .toList())
                                .build()
                        : branch)
                .toList();

        return franchiseRepository.save(franchise.toBuilder().branches(updatedBranches).build());
    }

    public record RemoveProductFromBranchRequest(String productId) {}
}
