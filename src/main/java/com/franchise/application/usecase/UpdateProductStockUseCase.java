package com.franchise.application.usecase;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public class UpdateProductStockUseCase {

    private final FranchiseRepository franchiseRepository;

    public UpdateProductStockUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<Franchise> execute(UpdateProductStockRequest request) {
        return Mono.fromCallable(() -> franchiseRepository.findById(request.franchiseId())
                .orElseThrow(() -> new IllegalArgumentException("Franchise not found")))
                .flatMap(franchise -> {
                    Franchise updatedFranchise = updateFranchise(franchise, request);
                    return Mono.fromCallable(() -> franchiseRepository.save(updatedFranchise));
                });
    }

    private Franchise updateFranchise(Franchise franchise, UpdateProductStockRequest request) {
        List<Branch> updatedBranches = franchise.getBranches().stream()
                .map(branch -> branch.getId().equals(request.branchId())
                        ? updateBranch(branch, request)
                        : branch)
                .collect(Collectors.toList());
        return franchise.toBuilder().branches(updatedBranches).build();
    }

    private Branch updateBranch(Branch branch, UpdateProductStockRequest request) {
        List<Product> updatedProducts = branch.getProducts().stream()
                .map(product -> product.getId().equals(request.productId())
                        ? product.toBuilder().stock(request.newStock()).build()
                        : product)
                .collect(Collectors.toList());
        return branch.toBuilder().products(updatedProducts).build();
    }

    public record UpdateProductStockRequest(String franchiseId, String branchId, String productId, int newStock) {}
}