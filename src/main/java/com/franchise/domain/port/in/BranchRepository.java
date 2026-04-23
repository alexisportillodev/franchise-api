package com.franchise.domain.port.in;

import com.franchise.domain.model.Branch;
import java.util.Optional;

public interface BranchRepository {
    Optional<Branch> findById(String id);
    Branch save(Branch branch);
    void deleteById(String id);
}