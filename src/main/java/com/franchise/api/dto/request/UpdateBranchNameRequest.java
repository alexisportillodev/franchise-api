package com.franchise.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateBranchNameRequest(
    @NotBlank(message = "Name is required")
    String name
) {}