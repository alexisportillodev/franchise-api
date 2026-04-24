package com.franchise.api.mapper;

import com.franchise.api.dto.response.ProductResponse;
import com.franchise.domain.model.Product;

public class ProductMapper {

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getStock()
        );
    }

    public static Product toDomain(com.franchise.api.dto.request.ProductRequest request) {
        return Product.builder()
            .name(request.name())
            .stock(request.stock())
            .build();
    }
}
