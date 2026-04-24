package com.franchise.api.dto.request;

import jakarta.validation.constraints.Min;

public record UpdateProductStockRequest(
    @Min(value = 0, message = "Stock must be non-negative")
    int stock
) {}