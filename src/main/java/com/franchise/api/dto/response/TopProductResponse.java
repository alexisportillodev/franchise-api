package com.franchise.api.dto.response;

public record TopProductResponse(
    String branchName,
    String productName,
    int stock
) {}