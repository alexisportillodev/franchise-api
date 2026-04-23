package com.franchise.api.mapper;

import com.franchise.api.dto.response.ProductResponse;
import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;

public class ProductMapper {

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getStock()
        );
    }

    public static ProductResponse toResponse(Franchise franchise, String productId) {
        return franchise.getBranches().stream()
            .flatMap(branch -> branch.getProducts().stream())
            .filter(product -> product.getId().equals(productId))
            .findFirst()
            .map(ProductMapper::toResponse)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    public static ProductResponse toResponse(Franchise franchise, String branchId, String productName, int stock) {
        Branch branch = franchise.getBranches().stream()
            .filter(candidate -> candidate.getId().equals(branchId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

        return branch.getProducts().stream()
            .filter(product -> product.getName().equals(productName) && product.getStock() == stock)
            .reduce((previous, current) -> current)
            .map(ProductMapper::toResponse)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    public static Product toDomain(com.franchise.api.dto.request.ProductRequest request) {
        return Product.builder()
            .name(request.name())
            .stock(request.stock())
            .build();
    }
}
