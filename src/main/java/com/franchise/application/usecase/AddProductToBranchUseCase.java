package com.franchise.application.usecase;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

public class AddProductToBranchUseCase {

    private final FranchiseRepository franchiseRepository;

    public AddProductToBranchUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<Franchise> execute(AddProductToBranchRequest request) {
        return Mono.fromCallable(() -> franchiseRepository.findById(request.franchiseId())
                .orElseThrow(() -> new IllegalArgumentException("Franchise not found")))
                .flatMap(franchise -> {
                    List<Branch> updatedBranches = franchise.getBranches().stream()
                            .map(branch -> branch.getId().equals(request.branchId())
                                    ? addProductToBranch(branch, request.productName(), request.stock())
                                    : branch)
                            .collect(Collectors.toList());
                    Franchise updatedFranchise = franchise.toBuilder().branches(updatedBranches).build();
                    return Mono.fromCallable(() -> franchiseRepository.save(updatedFranchise));
                });
    }

    private Branch addProductToBranch(Branch branch, String productName, int stock) {
        Product newProduct = Product.builder()
                .id(generateId())
                .name(productName)
                .stock(stock)
                .build();
        List<Product> updatedProducts = new ArrayList<>(branch.getProducts());
        updatedProducts.add(newProduct);
        return branch.toBuilder().products(updatedProducts).build();
    }

    private String generateId() {
        return java.util.UUID.randomUUID().toString();
    }

    public record AddProductToBranchRequest(String franchiseId, String branchId, String productName, int stock) {}
}