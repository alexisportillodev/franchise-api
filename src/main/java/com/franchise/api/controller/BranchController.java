package com.franchise.api.controller;

import com.franchise.api.dto.request.ProductRequest;
import com.franchise.api.dto.request.UpdateBranchNameRequest;
import com.franchise.api.dto.response.BranchResponse;
import com.franchise.api.dto.response.ProductResponse;
import com.franchise.api.mapper.BranchMapper;
import com.franchise.api.mapper.ProductMapper;
import com.franchise.application.usecase.branch.UpdateBranchNameUseCase;
import com.franchise.application.usecase.product.AddProductToBranchUseCase;
import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/branches")
public class BranchController {

    private final UpdateBranchNameUseCase updateBranchNameUseCase;
    private final AddProductToBranchUseCase addProductToBranchUseCase;

    public BranchController(UpdateBranchNameUseCase updateBranchNameUseCase,
                            AddProductToBranchUseCase addProductToBranchUseCase) {
        this.updateBranchNameUseCase = updateBranchNameUseCase;
        this.addProductToBranchUseCase = addProductToBranchUseCase;
    }

    @PutMapping("/{id}")
    public Mono<BranchResponse> updateBranchName(@PathVariable String id,
                                                 @Valid @RequestBody UpdateBranchNameRequest request) {
        return updateBranchNameUseCase.execute(
                new UpdateBranchNameUseCase.UpdateBranchNameRequest(id, request.name())
        ).map(franchise -> extractBranch(franchise, id))
         .map(BranchMapper::toResponse);
    }

    @PostMapping("/{id}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductResponse> addProductToBranch(@PathVariable String id,
                                                    @Valid @RequestBody ProductRequest request) {
        return addProductToBranchUseCase.execute(
                new AddProductToBranchUseCase.AddProductToBranchRequest(id, request.name(), request.stock())
        ).map(franchise -> extractCreatedProduct(franchise, id, request))
         .map(ProductMapper::toResponse);
    }

    private Branch extractBranch(Franchise franchise, String branchId) {
        return franchise.getBranches().stream()
                .filter(branch -> branch.getId().equals(branchId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
    }

    private Product extractCreatedProduct(Franchise franchise, String branchId, ProductRequest request) {
        return franchise.getBranches().stream()
                .filter(branch -> branch.getId().equals(branchId))
                .findFirst()
                .flatMap(branch -> branch.getProducts().stream()
                        .filter(product -> product.getName().equals(request.name())
                                && product.getStock() == request.stock())
                        .findFirst()
                )
                .orElseThrow(() -> new IllegalArgumentException("Created product not found"));
    }
}
