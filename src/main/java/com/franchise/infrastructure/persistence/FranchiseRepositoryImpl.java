package com.franchise.infrastructure.persistence;

import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.FranchiseRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FranchiseRepositoryImpl implements FranchiseRepository {

    // SimulaciÃ³n in-memory para ejemplo (en producciÃ³n, usar DynamoDB con Reactor)
    private final ConcurrentHashMap<String, Franchise> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> branchToFranchiseIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProductLocation> productIndex = new ConcurrentHashMap<>();

    @Override
    public Optional<Franchise> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<BranchLocation> findBranchLocation(String branchId) {
        return Optional.ofNullable(branchToFranchiseIndex.get(branchId))
                .map(franchiseId -> new BranchLocation(franchiseId, branchId));
    }

    @Override
    public Optional<ProductLocation> findProductLocation(String productId) {
        return Optional.ofNullable(productIndex.get(productId));
    }

    @Override
    public Franchise save(Franchise franchise) {
        Franchise previous = store.put(franchise.getId(), franchise);
        if (previous != null) {
            deindex(previous);
        }

        index(franchise);
        return franchise;
    }

    @Override
    public void deleteById(String id) {
        Franchise removed = store.remove(id);
        if (removed != null) {
            deindex(removed);
        }
    }

    @Override
    public List<Franchise> findAll() {
        return List.copyOf(store.values());
    }

    private void index(Franchise franchise) {
        franchise.getBranches().forEach(branch -> {
            branchToFranchiseIndex.put(branch.getId(), franchise.getId());
            branch.getProducts().forEach(product -> indexProduct(franchise.getId(), branch.getId(), product));
        });
    }

    private void indexProduct(String franchiseId, String branchId, Product product) {
        productIndex.put(product.getId(), new ProductLocation(franchiseId, branchId, product.getId()));
    }

    private void deindex(Franchise franchise) {
        franchise.getBranches().forEach(branch -> {
            branchToFranchiseIndex.remove(branch.getId(), franchise.getId());
            branch.getProducts().forEach(product -> productIndex.remove(product.getId()));
        });
    }

    // Nota: En implementaciÃ³n real con DynamoDB, usar Mono.fromFuture para operaciones asÃ­ncronas
    // Ejemplo: Mono.fromFuture(dynamoDbClient.getItem(request)).map(response -> mapToFranchise(response))
}
