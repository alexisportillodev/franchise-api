package com.franchise.application.usecase.product;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

public class FindProductLocationUseCase {

    private final FranchiseRepository franchiseRepository;

    public FindProductLocationUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<ProductLocation> execute(FindProductLocationRequest request) {
        return Mono.fromSupplier(() -> franchiseRepository.findAll().stream()
                .map(franchise -> locateProduct(franchise, request.productId()))
                .filter(location -> location != null)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not found")));
    }

    private ProductLocation locateProduct(Franchise franchise, String productId) {
        return franchise.getBranches().stream()
                .filter(branch -> containsProduct(branch, productId))
                .findFirst()
                .map(branch -> new ProductLocation(franchise.getId(), branch.getId(), productId))
                .orElse(null);
    }

    private boolean containsProduct(Branch branch, String productId) {
        return branch.getProducts().stream().anyMatch(product -> product.getId().equals(productId));
    }

    public record FindProductLocationRequest(String productId) {}

    public record ProductLocation(String franchiseId, String branchId, String productId) {}
}
