package com.franchise.api.mapper;

import com.franchise.api.dto.response.FranchiseResponse;
import com.franchise.domain.model.Franchise;
import java.util.List;
import java.util.stream.Collectors;

public class FranchiseMapper {

    public static FranchiseResponse toResponse(Franchise franchise) {
        List<com.franchise.api.dto.response.BranchResponse> branches = franchise.getBranches().stream()
            .map(BranchMapper::toResponse)
            .collect(Collectors.toList());
        return new FranchiseResponse(
            franchise.getId(),
            franchise.getName(),
            branches
        );
    }

    public static Franchise toDomain(com.franchise.api.dto.request.FranchiseRequest request) {
        return Franchise.builder()
            .name(request.name())
            .branches(List.of())
            .build();
    }
}