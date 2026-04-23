package com.franchise.application.usecase.product;

import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

public class FindProductLocationUseCase {

    private final FranchiseRepository franchiseRepository;

    public FindProductLocationUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<ProductLocation> execute(FindProductLocationRequest request) {
        return Mono.fromSupplier(() -> franchiseRepository.findProductLocation(request.productId())
                .map(location -> new ProductLocation(location.franchiseId(), location.branchId(), location.productId()))
                .orElseThrow(() -> new IllegalArgumentException("Product not found")));
    }

    public record FindProductLocationRequest(String productId) {}

    public record ProductLocation(String franchiseId, String branchId, String productId) {}
}
