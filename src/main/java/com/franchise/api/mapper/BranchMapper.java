package com.franchise.api.mapper;

import com.franchise.api.dto.response.BranchResponse;
import com.franchise.domain.model.Branch;
import java.util.List;
import java.util.stream.Collectors;

public class BranchMapper {

    public static BranchResponse toResponse(Branch branch) {
        List<com.franchise.api.dto.response.ProductResponse> products = branch.getProducts().stream()
            .map(ProductMapper::toResponse)
            .collect(Collectors.toList());
        return new BranchResponse(
            branch.getId(),
            branch.getName(),
            products
        );
    }

    public static Branch toDomain(com.franchise.api.dto.request.BranchRequest request) {
        return Branch.builder()
            .name(request.name())
            .products(List.of())
            .build();
    }
}