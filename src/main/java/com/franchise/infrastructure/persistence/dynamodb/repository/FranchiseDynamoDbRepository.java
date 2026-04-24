package com.franchise.infrastructure.persistence.dynamodb.repository;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.FranchiseRepository;
import com.franchise.infrastructure.persistence.dynamodb.DynamoDbRepositoryException;
import com.franchise.infrastructure.persistence.dynamodb.mapper.BranchMapper;
import com.franchise.infrastructure.persistence.dynamodb.mapper.FranchiseMapper;
import com.franchise.infrastructure.persistence.dynamodb.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.BRANCH_PK_PREFIX;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.FRANCHISE_PK_PREFIX;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.branchSk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.franchisePk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.productPk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.productSk;

/**
 * DynamoDB-backed implementation of {@link FranchiseRepository}.
 *
 * <p>Uses a single-table design where Franchise, Branch, and Product entities
 * are stored in the same table using composite PK/SK keys. A GSI ({@code GSI_SK_PK})
 * enables reverse lookups from child IDs to parent IDs.</p>
 *
 * <p>All {@link java.util.concurrent.CompletableFuture#join()} calls are
 * intentionally blocking; callers must invoke this repository from a
 * {@code Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())}
 * context to avoid blocking the Netty event loop.</p>
 *
 * <p>Requirements: 3.1–3.11</p>
 */
@Repository
public class FranchiseDynamoDbRepository implements FranchiseRepository {

    private static final String GSI_SK_PK = "GSI_SK_PK";

    private final DynamoDbAsyncClient dynamoDbClient;
    private final FranchiseMapper franchiseMapper;
    private final BranchMapper branchMapper;
    private final ProductMapper productMapper;
    private final String tableName;

    public FranchiseDynamoDbRepository(
            DynamoDbAsyncClient dynamoDbClient,
            FranchiseMapper franchiseMapper,
            BranchMapper branchMapper,
            ProductMapper productMapper,
            @Qualifier("dynamoDbTableName") String dynamoDbTableName) {

        this.dynamoDbClient  = dynamoDbClient;
        this.franchiseMapper = franchiseMapper;
        this.branchMapper    = branchMapper;
        this.productMapper   = productMapper;
        this.tableName       = dynamoDbTableName;

        System.out.println(">>> [REPOSITORY] INIT");
        System.out.println(">>> TABLE NAME INJECTED: " + this.tableName);
        System.out.println(">>> CLIENT: " + dynamoDbClient.getClass().getName());
    }

    // -------------------------------------------------------------------------
    // FranchiseRepository implementation
    // -------------------------------------------------------------------------

    /**
     * Saves a franchise and all its nested branches and products atomically
     * via a single {@code TransactWriteItems} call.
     *
     * @param franchise the franchise to save
     * @return the saved franchise (unchanged)
     */
    @Override
    public Franchise save(Franchise franchise) {
        List<TransactWriteItem> writeItems = new ArrayList<>();

        // Put franchise item
        writeItems.add(putItem(franchiseMapper.toItem(franchise)));

        // Put each branch item and its products
        for (Branch branch : franchise.getBranches()) {
            writeItems.add(putItem(branchMapper.toItem(franchise.getId(), branch)));
            for (Product product : branch.getProducts()) {
                writeItems.add(putItem(productMapper.toItem(branch.getId(), product)));
            }
        }

        TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
                .transactItems(writeItems)
                .build();

        try {
            dynamoDbClient.transactWriteItems(request).join();
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }

        return franchise;
    }

    /**
     * Finds a franchise by ID, reconstructing the full object graph.
     *
     * @param id the franchise ID
     * @return an {@link Optional} containing the franchise, or empty if not found
     */
    @Override
public Optional<Franchise> findById(String id) {

    System.out.println(">>> [findById] START");
    System.out.println(">>> TABLE: " + tableName);
    System.out.println(">>> ID: " + id);

    QueryRequest request = QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("PK = :pk")
            .expressionAttributeValues(Map.of(
                    ":pk", AttributeValue.builder().s(franchisePk(id)).build()
            ))
            .build();

    try {
        System.out.println(">>> EXECUTING QUERY findById");
        QueryResponse response = dynamoDbClient.query(request).join();

        System.out.println(">>> RESPONSE ITEMS: " + response.items().size());

        if (!response.hasItems() || response.items().isEmpty()) {
            System.out.println(">>> NO ITEMS FOUND");
            return Optional.empty();
        }

        return reconstructFranchise(id, response.items());

    } catch (CompletionException e) {
        System.err.println(">>> ERROR in findById TABLE=" + tableName);
        e.printStackTrace();
        throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
    }
}

    /**
     * Returns all franchises, each fully reconstructed with nested branches and products.
     *
     * @return list of all franchises
     */
    @Override
public List<Franchise> findAll() {

    System.out.println(">>> [findAll] START TABLE: " + tableName);

    ScanRequest request = ScanRequest.builder()
            .tableName(tableName)
            .filterExpression("entityType = :type")
            .expressionAttributeValues(Map.of(
                    ":type", AttributeValue.builder().s("FRANCHISE").build()
            ))
            .build();

    try {
        ScanResponse response = dynamoDbClient.scan(request).join();

        System.out.println(">>> SCAN ITEMS: " + response.items().size());

        if (!response.hasItems()) {
            return Collections.emptyList();
        }

        return response.items().stream()
                .map(item -> item.get("id").s())
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

    } catch (CompletionException e) {
        System.err.println(">>> ERROR in findAll TABLE=" + tableName);
        e.printStackTrace();
        throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
    }
}

    /**
     * Deletes a franchise and all its associated branches and products.
     *
     * @param id the franchise ID to delete
     */
    @Override
public void deleteById(String id) {

    System.out.println(">>> [deleteById] START ID=" + id);
    System.out.println(">>> TABLE=" + tableName);

    QueryRequest franchiseQuery = QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("PK = :pk")
            .expressionAttributeValues(Map.of(
                    ":pk", AttributeValue.builder().s(franchisePk(id)).build()
            ))
            .build();

    try {
        QueryResponse response = dynamoDbClient.query(franchiseQuery).join();

        System.out.println(">>> DELETE QUERY ITEMS: " + response.items().size());

        if (response.items().isEmpty()) {
            System.out.println(">>> NOTHING TO DELETE");
            return;
        }

        // resto igual...

    } catch (CompletionException e) {
        System.err.println(">>> ERROR in deleteById TABLE=" + tableName);
        e.printStackTrace();
        throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
    }
}

    /**
     * Finds the franchise that owns a given branch via the GSI.
     *
     * @param branchId the branch ID to look up
     * @return an {@link Optional} containing the branch location, or empty if not found
     */
    @Override
    public Optional<BranchLocation> findBranchLocation(String branchId) {
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
            return Optional.of(new BranchLocation(franchiseId, branchId));
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }
    }

    /**
     * Finds the franchise and branch that own a given product via two GSI lookups.
     *
     * @param productId the product ID to look up
     * @return an {@link Optional} containing the product location, or empty if not found
     */
    @Override
    public Optional<ProductLocation> findProductLocation(String productId) {
        // Step 1: find branchId from productId via GSI
        QueryRequest productGsiRequest = QueryRequest.builder()
                .tableName(tableName)
                .indexName(GSI_SK_PK)
                .keyConditionExpression("SK = :sk")
                .expressionAttributeValues(Map.of(
                        ":sk", AttributeValue.builder().s(productSk(productId)).build()
                ))
                .build();

        QueryResponse productGsiResponse;
        try {
            productGsiResponse = dynamoDbClient.query(productGsiRequest).join();
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }

        if (!productGsiResponse.hasItems() || productGsiResponse.items().isEmpty()) {
            return Optional.empty();
        }

        AttributeValue branchPkAttr = productGsiResponse.items().get(0).get("PK");
        if (branchPkAttr == null) {
            return Optional.empty();
        }
        String branchPk = branchPkAttr.s();
        String branchId = branchPk.startsWith(BRANCH_PK_PREFIX)
                ? branchPk.substring(BRANCH_PK_PREFIX.length())
                : branchPk;

        // Step 2: find franchiseId from branchId via GSI
        QueryRequest branchGsiRequest = QueryRequest.builder()
                .tableName(tableName)
                .indexName(GSI_SK_PK)
                .keyConditionExpression("SK = :sk")
                .expressionAttributeValues(Map.of(
                        ":sk", AttributeValue.builder().s(branchSk(branchId)).build()
                ))
                .build();

        QueryResponse branchGsiResponse;
        try {
            branchGsiResponse = dynamoDbClient.query(branchGsiRequest).join();
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }

        if (!branchGsiResponse.hasItems() || branchGsiResponse.items().isEmpty()) {
            return Optional.empty();
        }

        AttributeValue franchisePkAttr = branchGsiResponse.items().get(0).get("PK");
        if (franchisePkAttr == null) {
            return Optional.empty();
        }
        String franchisePkStr = franchisePkAttr.s();
        String franchiseId = franchisePkStr.startsWith(FRANCHISE_PK_PREFIX)
                ? franchisePkStr.substring(FRANCHISE_PK_PREFIX.length())
                : franchisePkStr;

        return Optional.of(new ProductLocation(franchiseId, branchId, productId));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Optional<Franchise> reconstructFranchise(String franchiseId,
                                                      List<Map<String, AttributeValue>> items) {
        Map<String, AttributeValue> franchiseItem = null;
        List<Map<String, AttributeValue>> branchItems = new ArrayList<>();

        for (Map<String, AttributeValue> item : items) {
            String entityType = item.get("entityType") != null ? item.get("entityType").s() : "";
            if ("FRANCHISE".equals(entityType)) {
                franchiseItem = item;
            } else if ("BRANCH".equals(entityType)) {
                branchItems.add(item);
            }
        }

        if (franchiseItem == null) {
            return Optional.empty();
        }

        List<Branch> branches = new ArrayList<>();
        for (Map<String, AttributeValue> branchItem : branchItems) {
            String branchId = branchItem.get("id").s();
            List<Product> products = queryProductsForBranch(branchId);
            branches.add(branchMapper.fromItem(branchItem, products));
        }

        return Optional.of(franchiseMapper.fromItem(franchiseItem, branches));
    }

    private List<Product> queryProductsForBranch(String branchId) {
        return queryProductItemsForBranch(branchId).stream()
                .map(productMapper::fromItem)
                .collect(Collectors.toList());
    }

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
            return response.hasItems() ? response.items() : List.of();
        } catch (CompletionException e) {
            throw new DynamoDbRepositoryException("DynamoDB operation failed", e.getCause());
        }
    }

    private TransactWriteItem putItem(Map<String, AttributeValue> item) {
        return TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(tableName)
                        .item(item)
                        .build())
                .build();
    }

    private TransactWriteItem deleteItem(String pk, String sk) {
        return TransactWriteItem.builder()
                .delete(Delete.builder()
                        .tableName(tableName)
                        .key(Map.of(
                                "PK", AttributeValue.builder().s(pk).build(),
                                "SK", AttributeValue.builder().s(sk).build()
                        ))
                        .build())
                .build();
    }
}
