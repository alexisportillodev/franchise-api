package com.franchise.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProductRequest(
    @NotBlank(message = "Name is required")
    String name,
    @Min(value = 0, message = "Stock must be non-negative")
    int stock
) {}