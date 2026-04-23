package com.franchise.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Product {
    String id;
    String name;
    int stock;
}
