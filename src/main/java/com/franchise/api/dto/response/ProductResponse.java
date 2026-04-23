package com.franchise.api.dto.response;

public record ProductResponse(
    String id,
    String name,
    int stock
) {}