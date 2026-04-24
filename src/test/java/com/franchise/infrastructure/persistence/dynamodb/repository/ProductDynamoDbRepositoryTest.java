package com.franchise.infrastructure.persistence.dynamodb.repository;

import com.franchise.domain.model.Product;
import com.franchise.infrastructure.persistence.dynamodb.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.productPk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.productSk;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProductDynamoDbRepository}.
 *
 * Uses a mocked {@link DynamoDbAsyncClient} and {@code CompletableFuture.completedFuture(...)}
 * to simulate async DynamoDB responses synchronously.
 *
 * Requirements: 5.2, 5.3, 5.5
 */
@ExtendWith(MockitoExtension.class)
class ProductDynamoDbRepositoryTest {

    private static final String TABLE_NAME = "franchise-table";
    private static final String BRANCH_ID  = "b-001";
    private static final String PRODUCT_ID = "p-001";

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    private ProductMapper productMapper;
    private ProductDynamoDbRepository repository;

    @BeforeEach
    void setUp() {
        productMapper = new ProductMapper();
        repository    = new ProductDynamoDbRepository(dynamoDbClient, productMapper, TABLE_NAME);
    }

    // -------------------------------------------------------------------------
    // findById tests
    // -------------------------------------------------------------------------

    /**
     * When the GSI query returns no items, findById must return Optional.empty()
     * without issuing a GetItem call.
     *
     * Requirements: 5.2
     */
    @Test
    void findById_returnsEmpty_whenGsiReturnsNoItems() {
        // GSI query returns empty result
        QueryResponse emptyGsiResponse = QueryResponse.builder()
                .items(Collections.emptyList())
                .build();
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(emptyGsiResponse));

        Optional<Product> result = repository.findById(PRODUCT_ID);

        assertThat(result).isEmpty();
        // GetItem must NOT be called when GSI finds nothing
        verify(dynamoDbClient, never()).getItem(any(GetItemRequest.class));
    }

    /**
     * When the GSI returns a branch PK but GetItem finds no item, findById
     * must return Optional.empty().
     *
     * Requirements: 5.2
     */
    @Test
    void findById_returnsEmpty_whenGetItemFindsNoItem() {
        stubGsiWithBranchPk(BRANCH_ID);

        GetItemResponse emptyGetItemResponse = GetItemResponse.builder()
                .item(Collections.emptyMap())
                .build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(emptyGetItemResponse));

        Optional<Product> result = repository.findById(PRODUCT_ID);

        assertThat(result).isEmpty();
    }

    /**
     * When both the GSI and GetItem return valid data, findById must return
     * the mapped product.
     *
     * Requirements: 5.2
     */
    @Test
    void findById_returnsProduct_whenFoundInDynamoDb() {
        Product expected = Product.builder()
                .id(PRODUCT_ID)
                .name("Classic Burger")
                .stock(42)
                .build();

        stubGsiWithBranchPk(BRANCH_ID);

        Map<String, AttributeValue> itemMap = productMapper.toItem(BRANCH_ID, expected);
        GetItemResponse getItemResponse = GetItemResponse.builder()
                .item(itemMap)
                .build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(getItemResponse));

        Optional<Product> result = repository.findById(PRODUCT_ID);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expected);
    }

    // -------------------------------------------------------------------------
    // deleteById tests
    // -------------------------------------------------------------------------

    /**
     * When the GSI returns no items, deleteById must be a no-op — no DeleteItem
     * call should be issued.
     *
     * Requirements: 5.5
     */
    @Test
    void deleteById_isNoOp_whenGsiReturnsNoItems() {
        QueryResponse emptyGsiResponse = QueryResponse.builder()
                .items(Collections.emptyList())
                .build();
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(emptyGsiResponse));

        repository.deleteById(PRODUCT_ID);

        verify(dynamoDbClient, never()).deleteItem(any(DeleteItemRequest.class));
    }

    /**
     * When the GSI returns a branch PK, deleteById must call DeleteItem with
     * the correct PK and SK derived from the resolved branchId and productId.
     *
     * Requirements: 5.5
     */
    @Test
    void deleteById_callsDeleteItem_withCorrectKeys_whenProductExists() {
        stubGsiWithBranchPk(BRANCH_ID);

        DeleteItemResponse deleteResponse = DeleteItemResponse.builder().build();
        when(dynamoDbClient.deleteItem(any(DeleteItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(deleteResponse));

        repository.deleteById(PRODUCT_ID);

        ArgumentCaptor<DeleteItemRequest> captor = ArgumentCaptor.forClass(DeleteItemRequest.class);
        verify(dynamoDbClient).deleteItem(captor.capture());

        DeleteItemRequest captured = captor.getValue();
        assertThat(captured.tableName()).isEqualTo(TABLE_NAME);
        assertThat(captured.key().get("PK").s()).isEqualTo(productPk(BRANCH_ID));
        assertThat(captured.key().get("SK").s()).isEqualTo(productSk(PRODUCT_ID));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Stubs the DynamoDB GSI query to return a single item whose {@code PK}
     * attribute is {@code "BRANCH#<branchId>"}.
     */
    private void stubGsiWithBranchPk(String branchId) {
        Map<String, AttributeValue> gsiItem = Map.of(
                "PK", AttributeValue.builder().s("BRANCH#" + branchId).build(),
                "SK", AttributeValue.builder().s(productSk(PRODUCT_ID)).build()
        );
        QueryResponse gsiResponse = QueryResponse.builder()
                .items(List.of(gsiItem))
                .build();
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(gsiResponse));
    }
}
