package com.franchise.infrastructure.persistence.dynamodb.repository;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.BranchRepository;
import com.franchise.infrastructure.persistence.dynamodb.DynamoDbRepositoryException;
import com.franchise.infrastructure.persistence.dynamodb.mapper.BranchMapper;
import com.franchise.infrastructure.persistence.dynamodb.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.FRANCHISE_PK_PREFIX;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.branchSk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.franchisePk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.productPk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.productSk;

/**
 * DynamoDB-backed implementation of {@link BranchRepository}.
 *
 * <p>Uses the {@code GSI_SK_PK} global secondary index to resolve the parent
 * franchise for a given branch ID, then performs the main-table operation.</p>
 *
 * <p>All {@link java.util.concurrent.CompletableFuture#join()} calls are
 * intentionally blocking; callers must invoke this repository from a
 * {@code Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())}
 * context to avoid blocking the Netty event loop.</p>
 *
 * <p>Requirements: 4.1–4.6</p>
 */
@Repository
public class BranchDynamoDbRepository implements BranchRepository {

    /** Name of the GSI used for reverse lookups (SK → PK). */
    private static final String GSI_SK_PK = "GSI_SK_PK";

    private final DynamoDbAsyncClient dynamoDbClient;
    private final BranchMapper branchMapper;
    private final ProductMapper productMapper;
    private final String tableName;

    public BranchDynamoDbRepository(
            DynamoDbAsyncClient dynamoDbClient,
            BranchMapper branchMapper,
            ProductMapper productMapper,
            @Qualifier("dynamoDbTableName") String dynamoDbTableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.branchMapper   = branchMapper;
        this.productMapper  = productMapper;
        this.tableName      = dynamoDbTableName;
    }

    // -------------------------------------------------------------------------
    // BranchRepository implementation
    // -------------------------------------------------------------------------

    /**
     * Finds a branch by its ID.
     *
     * <ol>
     *   <li>Queries {@code GSI_SK_PK} with {@code SK = "BRANCH#branchId"} to
     *       locate the parent franchise PK.</li>
     *   <li>Calls {@code GetItem} on the main table using the resolved franchise PK
     *       and the branch SK.</li>
     *   <li>Queries the main table for all products under
     *       {@code PK = "BRANCH#branchId"} with {@code SK begins_with "PRODUCT#"}.</li>
     *   <li>Reconstructs and returns the {@link Branch} with its products.</li>
     * </ol>
     *
     * @param branchId the branch ID to look up
     * @return an {@link Optional} containing the branch, or empty if not found
     */
    @Override
    public Optional<Branch> findById(String branchId) {
        Optional<String> franchiseIdOpt = findFranchiseIdByBranchId(branchId);
        if (franchiseIdOpt.isEmpty()) {
            return Optional.empty();
        }

        String franchiseId = franchiseIdOpt.get();

        // Fetch the branch item from the main table
        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "PK", AttributeValue.builder().s(franchisePk(franchiseId)).build(),
                        "SK", AttributeValue.builder().s(branchSk(branchId)).build()
                ))
                .build();

        GetItemResponse getItemResponse;
        try {
            getItemResponse = dynamoDbClient.getItem(getItemRequest).join();
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }

        if (!getItemResponse.hasItem() || getItemResponse.item().isEmpty()) {
            return Optional.empty();
        }

        // Fetch all products for this branch
        List<Product> products = queryProductsForBranch(branchId);

        Branch branch = branchMapper.fromItem(getItemResponse.item(), products);
        return Optional.of(branch);
    }

    /**
     * Saves (upserts) a branch and all its products atomically via
     * {@code TransactWriteItems}.
     *
     * <p>Performs a GSI lookup to resolve the parent {@code franchiseId} from the
     * existing item. If the branch does not yet exist in the table, throws
     * {@link DynamoDbRepositoryException} because the parent franchise cannot be
     * determined without the stored {@code franchiseId} attribute.</p>
     *
     * @param branch the branch to save
     * @return the saved branch (unchanged)
     * @throws DynamoDbRepositoryException if the branch is not found in the GSI
     */
    @Override
    public Branch save(Branch branch) {
        Optional<String> franchiseIdOpt = findFranchiseIdByBranchId(branch.getId());
        if (franchiseIdOpt.isEmpty()) {
            throw new DynamoDbRepositoryException("Branch not found: " + branch.getId());
        }

        String franchiseId = franchiseIdOpt.get();

        List<TransactWriteItem> writeItems = new ArrayList<>();

        // Put the branch item
        writeItems.add(TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(tableName)
                        .item(branchMapper.toItem(franchiseId, branch))
                        .build())
                .build());

        // Put each product item
        for (Product product : branch.getProducts()) {
            writeItems.add(TransactWriteItem.builder()
                    .put(Put.builder()
                            .tableName(tableName)
                            .item(productMapper.toItem(branch.getId(), product))
                            .build())
                    .build());
        }

        TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
                .transactItems(writeItems)
                .build();

        try {
            dynamoDbClient.transactWriteItems(request).join();
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }

        return branch;
    }

    /**
     * Deletes a branch and all its products atomically via
     * {@code TransactWriteItems}.
     *
     * <p>If the branch does not exist (GSI returns no items), this method is a
     * no-op and returns silently.</p>
     *
     * @param branchId the branch ID to delete
     */
    @Override
    public void deleteById(String branchId) {
        Optional<String> franchiseIdOpt = findFranchiseIdByBranchId(branchId);
        if (franchiseIdOpt.isEmpty()) {
            return; // no-op
        }

        String franchiseId = franchiseIdOpt.get();

        // Query all products for this branch
        List<Map<String, AttributeValue>> productItems = queryProductItemsForBranch(branchId);

        List<TransactWriteItem> deleteItems = new ArrayList<>();

        // Delete the branch item
        deleteItems.add(TransactWriteItem.builder()
                .delete(Delete.builder()
                        .tableName(tableName)
                        .key(Map.of(
                                "PK", AttributeValue.builder().s(franchisePk(franchiseId)).build(),
                                "SK", AttributeValue.builder().s(branchSk(branchId)).build()
                        ))
                        .build())
                .build());

        // Delete each product item
        for (Map<String, AttributeValue> productItem : productItems) {
            deleteItems.add(TransactWriteItem.builder()
                    .delete(Delete.builder()
                            .tableName(tableName)
                            .key(Map.of(
                                    "PK", AttributeValue.builder().s(productPk(branchId)).build(),
                                    "SK", productItem.get("SK")
                            ))
                            .build())
                    .build());
        }

        TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
                .transactItems(deleteItems)
                .build();

        try {
            dynamoDbClient.transactWriteItems(request).join();
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Queries {@code GSI_SK_PK} with {@code SK = "BRANCH#branchId"} to find
     * the parent franchise's PK value (e.g. {@code "FRANCHISE#f-001"}), then
     * strips the {@code "FRANCHISE#"} prefix to return the raw franchise ID.
     *
     * @param branchId the branch ID to look up
     * @return an {@link Optional} containing the raw franchise ID string, or
     *         empty if no item was found
     */
    private Optional<String> findFranchiseIdByBranchId(String branchId) {
        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .indexName(GSI_SK_PK)
                .keyConditionExpression("SK = :sk")
                .expressionAttributeValues(Map.of(
                        ":sk", AttributeValue.builder().s(branchSk(branchId)).build()
                ))
                .build();

        try {
            QueryResponse response = dynamoDbClient.query(request).join();
            if (!response.hasItems() || response.items().isEmpty()) {
                return Optional.empty();
            }
            AttributeValue pkAttr = response.items().get(0).get("PK");
            if (pkAttr == null) {
                return Optional.empty();
            }
            String pk = pkAttr.s();
            String franchiseId = pk.startsWith(FRANCHISE_PK_PREFIX)
                    ? pk.substring(FRANCHISE_PK_PREFIX.length())
                    : pk;
            return Optional.of(franchiseId);
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }
    }

    /**
     * Queries the main table for all product items under
     * {@code PK = "BRANCH#branchId"} with {@code SK begins_with "PRODUCT#"}.
     *
     * @param branchId the branch ID whose products to fetch
     * @return a list of mapped {@link Product} domain objects
     */
    private List<Product> queryProductsForBranch(String branchId) {
        return queryProductItemsForBranch(branchId).stream()
                .map(productMapper::fromItem)
                .collect(Collectors.toList());
    }

    /**
     * Queries the main table for all raw product attribute maps under
     * {@code PK = "BRANCH#branchId"} with {@code SK begins_with "PRODUCT#"}.
     *
     * @param branchId the branch ID whose product items to fetch
     * @return a list of raw DynamoDB attribute maps
     */
    private List<Map<String, AttributeValue>> queryProductItemsForBranch(String branchId) {
        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("PK = :pk AND begins_with(SK, :skPrefix)")
                .expressionAttributeValues(Map.of(
                        ":pk",       AttributeValue.builder().s(productPk(branchId)).build(),
                        ":skPrefix", AttributeValue.builder().s("PRODUCT#").build()
                ))
                .build();

        try {
            QueryResponse response = dynamoDbClient.query(request).join();
            if (!response.hasItems()) {
                return List.of();
            }
            return response.items();
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }
    }
}
