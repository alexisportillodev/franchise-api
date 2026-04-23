package com.franchise.domain.model;

import lombok.Builder;
import lombok.Value;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class Branch {
    String id;
    String name;
    List<Product> products;
}
