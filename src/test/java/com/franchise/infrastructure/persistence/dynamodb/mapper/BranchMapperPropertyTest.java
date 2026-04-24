package com.franchise.infrastructure.persistence.dynamodb.mapper;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Product;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.NotEmpty;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link BranchMapper}.
 *
 * Feature: aws-dynamodb-infrastructure
 */
class BranchMapperPropertyTest {

    private final BranchMapper   branchMapper  = new BranchMapper();
    private final ProductMapper  productMapper = new ProductMapper();

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
                                        validProducts().list().ofMinSize(0).ofMaxSize(5)
                                                .map(products -> Branch.builder()
                                                        .id(id)
                                                        .name(name)
                                                        .products(products)
                                                        .build())));
    }

    /**
     * Property 5: Branch mapper round-trip
     *
     * For any valid Branch (non-null id, non-null name, list of valid Products),
     * serialising to DynamoDB items and deserialising back produces a Branch equal to the original.
     *
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    // Feature: aws-dynamodb-infrastructure, Property 5: Branch mapper round-trip
    void branchMapperRoundTrip(
            @ForAll("validBranches") Branch branch,
            @ForAll @NotEmpty String franchiseId) {

        // Serialise branch metadata
        Map<String, AttributeValue> branchItem = branchMapper.toItem(franchiseId, branch);

        // Serialise each product and then deserialise them back
        List<Product> reconstructedProducts = branch.getProducts().stream()
                .map(product -> {
                    Map<String, AttributeValue> productItem = productMapper.toItem(branch.getId(), product);
                    return productMapper.fromItem(productItem);
                })
                .toList();

        // Reconstruct the branch
        Branch result = branchMapper.fromItem(branchItem, reconstructedProducts);

        assertThat(result).isEqualTo(branch);
    }
}
