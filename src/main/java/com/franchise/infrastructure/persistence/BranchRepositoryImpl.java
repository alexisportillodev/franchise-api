package com.franchise.infrastructure.persistence;

import com.franchise.domain.model.Branch;
import com.franchise.domain.port.in.BranchRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BranchRepositoryImpl implements BranchRepository {

    private final ConcurrentHashMap<String, Branch> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Branch> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Branch save(Branch branch) {
        store.put(branch.getId(), branch);
        return branch;
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}