package com.franchise.application.usecase.product;

import com.franchise.application.usecase.branch.FindBranchLocationUseCase;
import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddProductToBranchUseCase {

    private final FranchiseRepository franchiseRepository;
    private final FindBranchLocationUseCase findBranchLocationUseCase;

    public AddProductToBranchUseCase(FranchiseRepository franchiseRepository,
                                     FindBranchLocationUseCase findBranchLocationUseCase) {
        this.franchiseRepository = franchiseRepository;
        this.findBranchLocationUseCase = findBranchLocationUseCase;
    }

    public Mono<Franchise> execute(AddProductToBranchRequest request) {
        return findBranchLocationUseCase.execute(new FindBranchLocationUseCase.FindBranchLocationRequest(request.branchId()))
                .map(location -> addProduct(location.franchiseId(), request));
    }

    private Franchise addProduct(String franchiseId, AddProductToBranchRequest request) {
        Franchise franchise = franchiseRepository.findById(franchiseId)
                .orElseThrow(() -> new IllegalArgumentException("Franchise not found"));

        List<Branch> updatedBranches = franchise.getBranches().stream()
                .map(branch -> branch.getId().equals(request.branchId())
                        ? addProductToBranch(branch, request.productName(), request.stock())
                        : branch)
                .toList();

        return franchiseRepository.save(franchise.toBuilder().branches(updatedBranches).build());
    }

    private Branch addProductToBranch(Branch branch, String productName, int stock) {
        Product newProduct = Product.builder()
                .id(UUID.randomUUID().toString())
                .name(productName)
                .stock(stock)
                .build();

        List<Product> updatedProducts = new ArrayList<>(branch.getProducts());
        updatedProducts.add(newProduct);
        return branch.toBuilder().products(updatedProducts).build();
    }

    public record AddProductToBranchRequest(String branchId, String productName, int stock) {}
}
