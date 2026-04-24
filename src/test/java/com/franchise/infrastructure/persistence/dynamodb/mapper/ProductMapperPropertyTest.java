package com.franchise.infrastructure.persistence.dynamodb.mapper;

import com.franchise.domain.model.Product;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.NotEmpty;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link ProductMapper}.
 *
 * Feature: aws-dynamodb-infrastructure
 */
class ProductMapperPropertyTest {

    private final ProductMapper mapper = new ProductMapper();

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
     * Property 4: Product mapper round-trip
     *
     * For any valid Product (non-null id, non-null name, non-negative stock),
     * mapper.fromItem(mapper.toItem(branchId, product)) equals the original product.
     *
     * Validates: Requirements 6.5
     */
    @Property(tries = 100)
    // Feature: aws-dynamodb-infrastructure, Property 4: Product mapper round-trip
    void productMapperRoundTrip(
            @ForAll("validProducts") Product product,
            @ForAll @NotEmpty String branchId) {

        Map<String, AttributeValue> item = mapper.toItem(branchId, product);
        Product result = mapper.fromItem(item);

        assertThat(result).isEqualTo(product);
    }
}
