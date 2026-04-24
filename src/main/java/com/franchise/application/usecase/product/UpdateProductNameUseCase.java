package com.franchise.application.usecase.product;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.List;

public class UpdateProductNameUseCase {

    private final FranchiseRepository franchiseRepository;
    private final FindProductLocationUseCase findProductLocationUseCase;

    public UpdateProductNameUseCase(FranchiseRepository franchiseRepository,
                                    FindProductLocationUseCase findProductLocationUseCase) {
        this.franchiseRepository = franchiseRepository;
        this.findProductLocationUseCase = findProductLocationUseCase;
    }

    public Mono<Franchise> execute(UpdateProductNameRequest request) {
        return findProductLocationUseCase.execute(new FindProductLocationUseCase.FindProductLocationRequest(request.productId()))
                .map(location -> updateProductName(location.franchiseId(), location.branchId(), request));
    }

    private Franchise updateProductName(String franchiseId, String branchId, UpdateProductNameRequest request) {
        Franchise franchise = franchiseRepository.findById(franchiseId)
                .orElseThrow(() -> new IllegalArgumentException("Franchise not found"));

        List<Branch> updatedBranches = franchise.getBranches().stream()
                .map(branch -> branch.getId().equals(branchId)
                        ? updateProduct(branch, request)
                        : branch)
                .toList();

        return franchiseRepository.save(franchise.toBuilder().branches(updatedBranches).build());
    }

    private Branch updateProduct(Branch branch, UpdateProductNameRequest request) {
        List<Product> updatedProducts = branch.getProducts().stream()
                .map(product -> product.getId().equals(request.productId())
                        ? product.toBuilder().name(request.newName()).build()
                        : product)
                .toList();
        return branch.toBuilder().products(updatedProducts).build();
    }

    public record UpdateProductNameRequest(String productId, String newName) {}
}
