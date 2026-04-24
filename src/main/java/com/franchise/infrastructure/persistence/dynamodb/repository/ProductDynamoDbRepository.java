package com.franchise.infrastructure.persistence.dynamodb.repository;

import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.ProductRepository;
import com.franchise.infrastructure.persistence.dynamodb.DynamoDbRepositoryException;
import com.franchise.infrastructure.persistence.dynamodb.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.BRANCH_PK_PREFIX;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.productPk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.productSk;

/**
 * DynamoDB-backed implementation of {@link ProductRepository}.
 *
 * <p>Uses the {@code GSI_SK_PK} global secondary index to resolve the parent
 * branch for a given product ID, then performs the main-table operation.</p>
 *
 * <p>All {@link java.util.concurrent.CompletableFuture#join()} calls are
 * intentionally blocking; callers must invoke this repository from a
 * {@code Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())}
 * context to avoid blocking the Netty event loop.</p>
 */
@Repository
public class ProductDynamoDbRepository implements ProductRepository {

    /** Name of the GSI used for reverse lookups (SK → PK). */
    private static final String GSI_SK_PK = "GSI_SK_PK";

    private final DynamoDbAsyncClient dynamoDbClient;
    private final ProductMapper productMapper;
    private final String tableName;

    public ProductDynamoDbRepository(
            DynamoDbAsyncClient dynamoDbClient,
            ProductMapper productMapper,
            @Qualifier("dynamoDbTableName") String dynamoDbTableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.productMapper  = productMapper;
        this.tableName      = dynamoDbTableName;
    }

    // -------------------------------------------------------------------------
    // ProductRepository implementation
    // -------------------------------------------------------------------------

    /**
     * Finds a product by its ID.
     *
     * <ol>
     *   <li>Queries {@code GSI_SK_PK} with {@code SK = "PRODUCT#productId"} to
     *       locate the parent branch PK.</li>
     *   <li>Calls {@code GetItem} on the main table using the resolved PK and SK.</li>
     * </ol>
     *
     * @param productId the product ID to look up
     * @return an {@link Optional} containing the product, or empty if not found
     */
    @Override
    public Optional<Product> findById(String productId) {
        Optional<String> branchPkOpt = findBranchPkByProductId(productId);
        if (branchPkOpt.isEmpty()) {
            return Optional.empty();
        }

        // Strip "BRANCH#" prefix to get the raw branchId
        String branchPk = branchPkOpt.get();
        String branchId = branchPk.startsWith(BRANCH_PK_PREFIX)
                ? branchPk.substring(BRANCH_PK_PREFIX.length())
                : branchPk;

        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "PK", AttributeValue.builder().s(productPk(branchId)).build(),
                        "SK", AttributeValue.builder().s(productSk(productId)).build()
                ))
                .build();

        try {
            GetItemResponse response = dynamoDbClient.getItem(request).join();
            if (!response.hasItem() || response.item().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(productMapper.fromItem(response.item()));
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }
    }

    /**
     * Saves (upserts) a product.
     *
     * <p>Performs a GSI lookup to resolve the parent {@code branchId} from the
     * existing item. If the product does not yet exist in the table, throws
     * {@link DynamoDbRepositoryException} because the parent branch cannot be
     * determined without the stored {@code branchId} attribute.</p>
     *
     * @param product the product to save
     * @return the saved product (unchanged)
     * @throws DynamoDbRepositoryException if the product is not found in the GSI
     */
    @Override
    public Product save(Product product) {
        Optional<String> branchPkOpt = findBranchPkByProductId(product.getId());
        if (branchPkOpt.isEmpty()) {
            throw new DynamoDbRepositoryException("Product not found: " + product.getId());
        }

        String branchPk = branchPkOpt.get();
        String branchId = branchPk.startsWith(BRANCH_PK_PREFIX)
                ? branchPk.substring(BRANCH_PK_PREFIX.length())
                : branchPk;

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(productMapper.toItem(branchId, product))
                .build();

        try {
            dynamoDbClient.putItem(request).join();
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }

        return product;
    }

    /**
     * Deletes a product by its ID.
     *
     * <p>If the product does not exist (GSI returns no items), this method is a
     * no-op and returns silently.</p>
     *
     * @param productId the product ID to delete
     */
    @Override
    public void deleteById(String productId) {
        Optional<String> branchPkOpt = findBranchPkByProductId(productId);
        if (branchPkOpt.isEmpty()) {
            return; // no-op
        }

        String branchPk = branchPkOpt.get();
        String branchId = branchPk.startsWith(BRANCH_PK_PREFIX)
                ? branchPk.substring(BRANCH_PK_PREFIX.length())
                : branchPk;

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "PK", AttributeValue.builder().s(productPk(branchId)).build(),
                        "SK", AttributeValue.builder().s(productSk(productId)).build()
                ))
                .build();

        try {
            dynamoDbClient.deleteItem(request).join();
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Queries {@code GSI_SK_PK} with {@code SK = "PRODUCT#productId"} to find
     * the parent branch's PK value (e.g. {@code "BRANCH#b-001"}).
     *
     * @param productId the product ID to look up
     * @return an {@link Optional} containing the full PK string (e.g.
     *         {@code "BRANCH#b-001"}), or empty if no item was found
     */
    private Optional<String> findBranchPkByProductId(String productId) {
        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .indexName(GSI_SK_PK)
                .keyConditionExpression("SK = :sk")
                .expressionAttributeValues(Map.of(
                        ":sk", AttributeValue.builder().s(productSk(productId)).build()
                ))
                .build();

        try {
            QueryResponse response = dynamoDbClient.query(request).join();
            if (!response.hasItems() || response.items().isEmpty()) {
                return Optional.empty();
            }
            // The GSI item's "PK" attribute holds the parent branch PK
            AttributeValue pkAttr = response.items().get(0).get("PK");
            if (pkAttr == null) {
                return Optional.empty();
            }
            return Optional.of(pkAttr.s());
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }
    }
}
