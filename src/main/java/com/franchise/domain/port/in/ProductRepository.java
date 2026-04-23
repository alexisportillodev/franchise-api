package com.franchise.domain.port.in;

import com.franchise.domain.model.Product;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(String id);
    Product save(Product product);
    void deleteById(String id);
}