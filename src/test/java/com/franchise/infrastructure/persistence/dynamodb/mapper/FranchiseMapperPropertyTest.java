package com.franchise.infrastructure.persistence.dynamodb.mapper;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link FranchiseMapper}.
 *
 * Feature: aws-dynamodb-infrastructure
 */
class FranchiseMapperPropertyTest {

    private final FranchiseMapper franchiseMapper = new FranchiseMapper();
    private final BranchMapper    branchMapper    = new BranchMapper();
    private final ProductMapper   productMapper   = new ProductMapper();

    /**
     * Provides valid {@link Product} instances: non-null id, non-null name, non-negative stock.
     */
    @Provide
    Arbitrary<Product> validProducts() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20)
                .flatMap(id ->
                        Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(50)
                                .flatMap(name ->
                                        Arbitraries.integers().between(0, Integer.MAX_VALUE)
                                                .map(stock -> Product.builder()
                                                        .id(id)
                                                        .name(name)
                                                        .stock(stock)
                                                        .build())));
    }

    /**
     * Provides valid {@link Branch} instances: non-null id, non-null name, list of valid Products.
     */
    @Provide
    Arbitrary<Branch> validBranches() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20)
                .flatMap(id ->
                        Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(50)
                                .flatMap(name ->
                                        validProducts().list().ofMinSize(0).ofMaxSize(3)
                                                .map(products -> Branch.builder()
                                                        .id(id)
                                                        .name(name)
                                                        .products(products)
                                                        .build())));
    }

    /**
     * Provides valid {@link Franchise} instances: non-null id, non-null name, list of valid Branches.
     */
    @Provide
    Arbitrary<Franchise> validFranchises() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20)
                .flatMap(id ->
                        Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(50)
                                .flatMap(name ->
                                        validBranches().list().ofMinSize(0).ofMaxSize(3)
                                                .map(branches -> Franchise.builder()
                                                        .id(id)
                                                        .name(name)
                                                        .branches(branches)
                                                        .build())));
    }

    /**
     * Property 6: Franchise mapper round-trip
     *
     * For any valid Franchise (non-null id, non-null name, list of valid Branches),
     * serialising to DynamoDB items and deserialising back produces a Franchise equal to the original.
     *
     * Validates: Requirements 6.3
     */
    @Property(tries = 100)
    // Feature: aws-dynamodb-infrastructure, Property 6: Franchise mapper round-trip
    void franchiseMapperRoundTrip(@ForAll("validFranchises") Franchise franchise) {

        // Serialise franchise metadata
        Map<String, AttributeValue> franchiseItem = franchiseMapper.toItem(franchise);

        // Serialise and deserialise each branch (with its products)
        List<Branch> reconstructedBranches = franchise.getBranches().stream()
                .map(branch -> {
                    Map<String, AttributeValue> branchItem = branchMapper.toItem(franchise.getId(), branch);

                    List<Product> reconstructedProducts = branch.getProducts().stream()
                            .map(product -> {
                                Map<String, AttributeValue> productItem = productMapper.toItem(branch.getId(), product);
                                return productMapper.fromItem(productItem);
                            })
                            .toList();

                    return branchMapper.fromItem(branchItem, reconstructedProducts);
                })
                .toList();

        // Reconstruct the franchise
        Franchise result = franchiseMapper.fromItem(franchiseItem, reconstructedBranches);

        assertThat(result).isEqualTo(franchise);
    }
}
