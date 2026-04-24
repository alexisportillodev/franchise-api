package com.franchise.infrastructure.persistence.dynamodb.repository;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Product;
import com.franchise.infrastructure.persistence.dynamodb.mapper.BranchMapper;
import com.franchise.infrastructure.persistence.dynamodb.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.branchSk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.franchisePk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.productPk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.productSk;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BranchDynamoDbRepository}.
 *
 * Uses a mocked {@link DynamoDbAsyncClient} and {@code CompletableFuture.completedFuture(...)}
 * to simulate async DynamoDB responses synchronously.
 *
 * Requirements: 4.2, 4.3, 4.5
 */
@ExtendWith(MockitoExtension.class)
class BranchDynamoDbRepositoryTest {

    private static final String TABLE_NAME    = "franchise-table";
    private static final String FRANCHISE_ID  = "f-001";
    private static final String BRANCH_ID     = "b-001";
    private static final String PRODUCT_ID_1  = "p-001";
    private static final String PRODUCT_ID_2  = "p-002";

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    private BranchMapper branchMapper;
    private ProductMapper productMapper;
    private BranchDynamoDbRepository repository;

    @BeforeEach
    void setUp() {
        branchMapper  = new BranchMapper();
        productMapper = new ProductMapper();
        repository    = new BranchDynamoDbRepository(dynamoDbClient, branchMapper, productMapper, TABLE_NAME);
    }

    // -------------------------------------------------------------------------
    // findById tests
    // -------------------------------------------------------------------------

    /**
     * When the GSI query returns no items, findById must return Optional.empty()
     * without issuing a GetItem call.
     *
     * Requirements: 4.2
     */
    @Test
    void findById_returnsEmpty_whenGsiReturnsNoItems() {
        // GSI query returns empty result
        QueryResponse emptyGsiResponse = QueryResponse.builder()
                .items(Collections.emptyList())
                .build();
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(emptyGsiResponse));

        Optional<Branch> result = repository.findById(BRANCH_ID);

        assertThat(result).isEmpty();
        // GetItem must NOT be called when GSI finds nothing
        verify(dynamoDbClient, never()).getItem(any(GetItemRequest.class));
    }

    // -------------------------------------------------------------------------
    // deleteById tests
    // -------------------------------------------------------------------------

    /**
     * When the GSI returns no items, deleteById must be a no-op — no
     * transactWriteItems call should be issued.
     *
     * Requirements: 4.5
     */
    @Test
    void deleteById_isNoOp_whenGsiReturnsNoItems() {
        QueryResponse emptyGsiResponse = QueryResponse.builder()
                .items(Collections.emptyList())
                .build();
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(emptyGsiResponse));

        repository.deleteById(BRANCH_ID);

        verify(dynamoDbClient, never()).transactWriteItems(any(TransactWriteItemsRequest.class));
    }

    /**
     * When the branch exists (GSI returns a franchise PK) and has products,
     * deleteById must call transactWriteItems with delete actions for the branch
     * item and all product items.
     *
     * Requirements: 4.3, 4.5
     */
    @Test
    void deleteById_callsTransactWriteItems_withCorrectDeletes_whenBranchExists() {
        // First query: GSI lookup returns franchise PK
        // Second query: product query returns two products
        Map<String, AttributeValue> gsiItem = Map.of(
                "PK", AttributeValue.builder().s(franchisePk(FRANCHISE_ID)).build(),
                "SK", AttributeValue.builder().s(branchSk(BRANCH_ID)).build()
        );
        QueryResponse gsiResponse = QueryResponse.builder()
                .items(List.of(gsiItem))
                .build();

        Map<String, AttributeValue> productItem1 = productMapper.toItem(BRANCH_ID,
                Product.builder().id(PRODUCT_ID_1).name("Burger").stock(10).build());
        Map<String, AttributeValue> productItem2 = productMapper.toItem(BRANCH_ID,
                Product.builder().id(PRODUCT_ID_2).name("Fries").stock(20).build());
        QueryResponse productQueryResponse = QueryResponse.builder()
                .items(List.of(productItem1, productItem2))
                .build();

        // First call = GSI lookup, second call = product query
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(gsiResponse))
                .thenReturn(CompletableFuture.completedFuture(productQueryResponse));

        TransactWriteItemsResponse transactResponse = TransactWriteItemsResponse.builder().build();
        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(transactResponse));

        repository.deleteById(BRANCH_ID);

        ArgumentCaptor<TransactWriteItemsRequest> captor =
                ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(dynamoDbClient).transactWriteItems(captor.capture());

        TransactWriteItemsRequest captured = captor.getValue();
        // Expect 3 delete actions: 1 branch + 2 products
        assertThat(captured.transactItems()).hasSize(3);

        // All items must be Delete actions
        assertThat(captured.transactItems())
                .allMatch(item -> item.delete() != null);

        // Verify branch delete key
        var branchDelete = captured.transactItems().get(0).delete();
        assertThat(branchDelete.tableName()).isEqualTo(TABLE_NAME);
        assertThat(branchDelete.key().get("PK").s()).isEqualTo(franchisePk(FRANCHISE_ID));
        assertThat(branchDelete.key().get("SK").s()).isEqualTo(branchSk(BRANCH_ID));

        // Verify product delete keys
        var productDelete1 = captured.transactItems().get(1).delete();
        assertThat(productDelete1.tableName()).isEqualTo(TABLE_NAME);
        assertThat(productDelete1.key().get("PK").s()).isEqualTo(productPk(BRANCH_ID));
        assertThat(productDelete1.key().get("SK").s()).isEqualTo(productSk(PRODUCT_ID_1));

        var productDelete2 = captured.transactItems().get(2).delete();
        assertThat(productDelete2.tableName()).isEqualTo(TABLE_NAME);
        assertThat(productDelete2.key().get("PK").s()).isEqualTo(productPk(BRANCH_ID));
        assertThat(productDelete2.key().get("SK").s()).isEqualTo(productSk(PRODUCT_ID_2));
    }
}
