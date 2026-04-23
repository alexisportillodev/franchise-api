package com.franchise.domain.port.in;

import com.franchise.domain.model.Franchise;
import java.util.List;
import java.util.Optional;

public interface FranchiseRepository {
    record BranchLocation(String franchiseId, String branchId) {}

    record ProductLocation(String franchiseId, String branchId, String productId) {}

    Optional<Franchise> findById(String id);
    Optional<BranchLocation> findBranchLocation(String branchId);
    Optional<ProductLocation> findProductLocation(String productId);
    Franchise save(Franchise franchise);
    void deleteById(String id);
    List<Franchise> findAll();
}
