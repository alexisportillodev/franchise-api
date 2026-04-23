package com.franchise.domain.port.in;

import com.franchise.domain.model.Franchise;
import java.util.List;
import java.util.Optional;

public interface FranchiseRepository {
    Optional<Franchise> findById(String id);
    Franchise save(Franchise franchise);
    void deleteById(String id);
    List<Franchise> findAll();
}