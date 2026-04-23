package com.franchise.infrastructure.persistence;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FranchiseRepositoryImplTest {

    private final FranchiseRepositoryImpl repository = new FranchiseRepositoryImpl();

    @Test
    void givenStoredFranchiseWhenLookingUpLocationsThenReturnIndexedMatches() {
        repository.save(franchise("fr-1", "br-1", "pr-1"));

        assertThat(repository.findBranchLocation("br-1"))
                .contains(new com.franchise.domain.port.in.FranchiseRepository.BranchLocation("fr-1", "br-1"));
        assertThat(repository.findProductLocation("pr-1"))
                .contains(new com.franchise.domain.port.in.FranchiseRepository.ProductLocation("fr-1", "br-1", "pr-1"));
    }

    @Test
    void givenUpdatedFranchiseWhenLookingUpOldProductThenIndexIsRefreshed() {
        repository.save(franchise("fr-1", "br-1", "pr-1"));
        repository.save(franchise("fr-1", "br-1", "pr-2"));

        assertThat(repository.findProductLocation("pr-1")).isEmpty();
        assertThat(repository.findProductLocation("pr-2"))
                .contains(new com.franchise.domain.port.in.FranchiseRepository.ProductLocation("fr-1", "br-1", "pr-2"));
    }

    @Test
    void givenDeletedFranchiseWhenLookingUpLocationsThenIndexesAreCleared() {
        repository.save(franchise("fr-1", "br-1", "pr-1"));

        repository.deleteById("fr-1");

        assertThat(repository.findBranchLocation("br-1")).isEmpty();
        assertThat(repository.findProductLocation("pr-1")).isEmpty();
    }

    private Franchise franchise(String franchiseId, String branchId, String productId) {
        Product product = Product.builder()
                .id(productId)
                .name("Producto")
                .stock(10)
                .build();

        Branch branch = Branch.builder()
                .id(branchId)
                .name("Sucursal")
                .products(List.of(product))
                .build();

        return Franchise.builder()
                .id(franchiseId)
                .name("Franquicia")
                .branches(List.of(branch))
                .build();
    }
}
