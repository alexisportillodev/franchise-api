package com.franchise.infrastructure.persistence.dynamodb.repository;

import com.franchise.domain.port.in.FranchiseRepository;
import com.franchise.infrastructure.persistence.dynamodb.mapper.BranchMapper;
import com.franchise.infrastructure.persistence.dynamodb.mapper.FranchiseMapper;
import com.franchise.infrastructure.persistence.dynamodb.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FranchiseDynamoDbRepository}.
 *
 * Uses a mocked {@link DynamoDbAsyncClient} and {@code CompletableFuture.completedFuture(...)}
 * to simulate async DynamoDB responses synchronously.
 *
 * Requirements: 3.4, 3.7, 3.9
 */
@ExtendWith(MockitoExtension.class)
class FranchiseDynamoDbRepositoryTest {

    private static final String TABLE_NAME   = "franchise";
    private static final String FRANCHISE_ID = "f-001";
    private static final String BRANCH_ID    = "b-001";
    private static final String PRODUCT_ID   = "p-001";

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    private FranchiseDynamoDbRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FranchiseDynamoDbRepository(
                dynamoDbClient,
                new FranchiseMapper(),
                new BranchMapper(),
                new ProductMapper(),
                TABLE_NAME
        );
    }

    /**
     * When the main-table query returns no items, findById must return Optional.empty().
     *
     * Requirements: 3.4
     */
    @Test
    void findById_returnsEmpty_whenQueryReturnsNoItems() {
        QueryResponse emptyResponse = QueryResponse.builder()
                .items(Collections.emptyList())
                .build();
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(emptyResponse));

        Optional<Object> result = repository.findById(FRANCHISE_ID).map(f -> f);

        assertThat(result).isEmpty();
    }

    /**
     * When the GSI query returns no items, findBranchLocation must return Optional.empty().
     *
     * Requirements: 3.7
     */
    @Test
    void findBranchLocation_returnsEmpty_whenGsiReturnsNoItems() {
        QueryResponse emptyResponse = QueryResponse.builder()
                .items(Collections.emptyList())
                .build();
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(emptyResponse));

        Optional<FranchiseRepository.BranchLocation> result = repository.findBranchLocation(BRANCH_ID);

        assertThat(result).isEmpty();
    }

    /**
     * When the GSI query returns no items for the product, findProductLocation must return Optional.empty().
     *
     * Requirements: 3.9
     */
    @Test
    void findProductLocation_returnsEmpty_whenGsiReturnsNoItems() {
        QueryResponse emptyResponse = QueryResponse.builder()
                .items(Collections.emptyList())
                .build();
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(emptyResponse));

        Optional<FranchiseRepository.ProductLocation> result = repository.findProductLocation(PRODUCT_ID);

        assertThat(result).isEmpty();
    }
}
