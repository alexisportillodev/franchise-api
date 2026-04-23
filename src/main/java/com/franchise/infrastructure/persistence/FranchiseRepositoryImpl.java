package com.franchise.infrastructure.persistence;

import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FranchiseRepositoryImpl implements FranchiseRepository {

    // Simulación in-memory para ejemplo (en producción, usar DynamoDB con Reactor)
    private final ConcurrentHashMap<String, Franchise> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Franchise> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Franchise save(Franchise franchise) {
        store.put(franchise.getId(), franchise);
        return franchise;
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }

    @Override
    public List<Franchise> findAll() {
        return List.copyOf(store.values());
    }

    // Nota: En implementación real con DynamoDB, usar Mono.fromFuture para operaciones asíncronas
    // Ejemplo: Mono.fromFuture(dynamoDbClient.getItem(request)).map(response -> mapToFranchise(response))
}