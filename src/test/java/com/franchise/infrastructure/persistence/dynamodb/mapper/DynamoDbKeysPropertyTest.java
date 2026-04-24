package com.franchise.infrastructure.persistence.dynamodb.mapper;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.NotEmpty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link DynamoDbKeys} key format methods.
 *
 * Feature: aws-dynamodb-infrastructure
 */
class DynamoDbKeysPropertyTest {

    /**
     * Property 1: Franchise key format
     *
     * For any non-empty franchise ID, franchisePk(id) equals "FRANCHISE#" + id
     * and franchiseSk(id) equals "FRANCHISE#" + id.
     *
     * Validates: Requirements 1.2
     */
    @Property(tries = 100)
    // Feature: aws-dynamodb-infrastructure, Property 1: Franchise key format
    void franchiseKeyFormat(@ForAll @NotEmpty String franchiseId) {
        assertThat(DynamoDbKeys.franchisePk(franchiseId))
                .isEqualTo("FRANCHISE#" + franchiseId);
        assertThat(DynamoDbKeys.franchiseSk(franchiseId))
                .isEqualTo("FRANCHISE#" + franchiseId);
    }

    /**
     * Property 2: Branch key format
     *
     * For any non-empty franchise ID and non-empty branch ID,
     * franchisePk(franchiseId) equals "FRANCHISE#" + franchiseId
     * and branchSk(branchId) equals "BRANCH#" + branchId.
     *
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    // Feature: aws-dynamodb-infrastructure, Property 2: Branch key format
    void branchKeyFormat(@ForAll @NotEmpty String franchiseId,
                         @ForAll @NotEmpty String branchId) {
        assertThat(DynamoDbKeys.franchisePk(franchiseId))
                .isEqualTo("FRANCHISE#" + franchiseId);
        assertThat(DynamoDbKeys.branchSk(branchId))
                .isEqualTo("BRANCH#" + branchId);
    }

    /**
     * Property 3: Product key format
     *
     * For any non-empty branch ID and non-empty product ID,
     * productPk(branchId) equals "BRANCH#" + branchId
     * and productSk(productId) equals "PRODUCT#" + productId.
     *
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    // Feature: aws-dynamodb-infrastructure, Property 3: Product key format
    void productKeyFormat(@ForAll @NotEmpty String branchId,
                          @ForAll @NotEmpty String productId) {
        assertThat(DynamoDbKeys.productPk(branchId))
                .isEqualTo("BRANCH#" + branchId);
        assertThat(DynamoDbKeys.productSk(productId))
                .isEqualTo("PRODUCT#" + productId);
    }
}
