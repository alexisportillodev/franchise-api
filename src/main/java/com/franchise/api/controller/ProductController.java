package com.franchise.api.controller;

import com.franchise.api.dto.request.UpdateProductNameRequest;
import com.franchise.api.dto.request.UpdateProductStockRequest;
import com.franchise.api.dto.response.ProductResponse;
import com.franchise.api.mapper.ProductMapper;
import com.franchise.application.usecase.product.RemoveProductFromBranchUseCase;
import com.franchise.application.usecase.product.UpdateProductNameUseCase;
import com.franchise.application.usecase.product.UpdateProductStockUseCase;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final RemoveProductFromBranchUseCase removeProductFromBranchUseCase;
    private final UpdateProductNameUseCase updateProductNameUseCase;
    private final UpdateProductStockUseCase updateProductStockUseCase;

    public ProductController(RemoveProductFromBranchUseCase removeProductFromBranchUseCase,
                             UpdateProductNameUseCase updateProductNameUseCase,
                             UpdateProductStockUseCase updateProductStockUseCase) {
        this.removeProductFromBranchUseCase = removeProductFromBranchUseCase;
        this.updateProductNameUseCase = updateProductNameUseCase;
        this.updateProductStockUseCase = updateProductStockUseCase;
    }

    @DeleteMapping("/{id}")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public Mono<Void> removeProduct(@PathVariable String id) {
        return removeProductFromBranchUseCase.execute(
                new RemoveProductFromBranchUseCase.RemoveProductFromBranchRequest(id)
        ).then();
    }

    @PutMapping("/{id}/stock")
    public Mono<ProductResponse> updateProductStock(@PathVariable String id,
                                                    @Valid @RequestBody UpdateProductStockRequest request) {
        return updateProductStockUseCase.execute(
                new UpdateProductStockUseCase.UpdateProductStockRequest(id, request.stock())
        )
        .map(franchise -> extractProduct(franchise, id))
        .map(ProductMapper::toResponse);
    }

    @PutMapping("/{id}/name")
    public Mono<ProductResponse> updateProductName(@PathVariable String id,
                                                   @Valid @RequestBody UpdateProductNameRequest request) {
        return updateProductNameUseCase.execute(
                new UpdateProductNameUseCase.UpdateProductNameRequest(id, request.name())
        )
        .map(franchise -> extractProduct(franchise, id))
        .map(ProductMapper::toResponse);
    }

    private Product extractProduct(Franchise franchise, String productId) {
        return franchise.getBranches().stream()
                .flatMap(branch -> branch.getProducts().stream())
                .filter(product -> product.getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }
}
