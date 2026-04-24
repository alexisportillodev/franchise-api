package com.franchise.infrastructure.persistence.dynamodb.mapper;

/**
 * Utility class holding DynamoDB key prefix constants and helper methods
 * for constructing composite PK/SK values used in the single-table design.
 */
public final class DynamoDbKeys {

    public static final String FRANCHISE_PK_PREFIX = "FRANCHISE#";
    public static final String BRANCH_SK_PREFIX    = "BRANCH#";
    public static final String PRODUCT_SK_PREFIX   = "PRODUCT#";
    public static final String BRANCH_PK_PREFIX    = "BRANCH#";

    private DynamoDbKeys() {
        // utility class — no instantiation
    }

    /**
     * Returns the partition key for a Franchise item: {@code "FRANCHISE#<franchiseId>"}.
     */
    public static String franchisePk(String franchiseId) {
        return FRANCHISE_PK_PREFIX + franchiseId;
    }

    /**
     * Returns the sort key for a Franchise item: {@code "FRANCHISE#<franchiseId>"}.
     */
    public static String franchiseSk(String franchiseId) {
        return FRANCHISE_PK_PREFIX + franchiseId;
    }

    /**
     * Returns the sort key for a Branch item: {@code "BRANCH#<branchId>"}.
     */
    public static String branchSk(String branchId) {
        return BRANCH_SK_PREFIX + branchId;
    }

    /**
     * Returns the partition key for a Product item: {@code "BRANCH#<branchId>"}.
     */
    public static String productPk(String branchId) {
        return BRANCH_PK_PREFIX + branchId;
    }

    /**
     * Returns the sort key for a Product item: {@code "PRODUCT#<productId>"}.
     */
    public static String productSk(String productId) {
        return PRODUCT_SK_PREFIX + productId;
    }
}
