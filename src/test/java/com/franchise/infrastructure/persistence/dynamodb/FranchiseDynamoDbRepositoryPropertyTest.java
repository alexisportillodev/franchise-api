package com.franchise.infrastructure.persistence.dynamodb;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.infrastructure.persistence.dynamodb.mapper.BranchMapper;
import com.franchise.infrastructure.persistence.dynamodb.mapper.FranchiseMapper;
import com.franchise.infrastructure.persistence.dynamodb.mapper.ProductMapper;
import com.franchise.infrastructure.persistence.dynamodb.repository.FranchiseDynamoDbRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based integration tests for {@link FranchiseDynamoDbRepository}
 * against a real DynamoDB Local instance provided by Testcontainers LocalStack.
 *
 * These tests are disabled automatically when Docker is not available.
 *
 * Feature: aws-dynamodb-infrastructure
 */
class FranchiseDynamoDbRepositoryPropertyTest {

    static boolean dockerUnavailable() {
        try {
            DockerClientFactory.instance().client();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static final String TABLE_NAME = "franchise-table";
    private static LocalStackContainer sharedLocalStack;

    private static synchronized LocalStackContainer getLocalStack() {
        if (sharedLocalStack == null) {
            sharedLocalStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
                    .withServices(LocalStackContainer.Service.DYNAMODB);
            sharedLocalStack.start();
        }
        return sharedLocalStack;
    }

    private DynamoDbAsyncClient buildClient() {
        LocalStackContainer ls = getLocalStack();
        return DynamoDbAsyncClient.builder()
                .endpointOverride(ls.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .region(Region.of(ls.getRegion()))
                .build();
    }

    private void createTable(DynamoDbAsyncClient client) {
        client.createTable(CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName("PK").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName("SK").attributeType(ScalarAttributeType.S).build()
                )
                .keySchema(
                        KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build()
                )
                .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                        .indexName("GSI_SK_PK")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("SK").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("PK").keyType(KeyType.RANGE).build()
                        )
                        .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                        .build())
                .build()).join();
    }

    @Provide
    Arbitrary<Franchise> validFranchises() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(8)
                .flatMap(name ->
                        Arbitraries.integers().between(0, 1000)
                                .map(stock -> {
                                    String fId = UUID.randomUUID().toString();
                                    String bId = UUID.randomUUID().toString();
                                    String pId = UUID.randomUUID().toString();
                                    Product p = Product.builder().id(pId).name(name + "-p").stock(stock).build();
                                    Branch b = Branch.builder().id(bId).name(name + "-b").products(List.of(p)).build();
                                    return Franchise.builder().id(fId).name(name).branches(List.of(b)).build();
                                }));
    }

    @Provide
    Arbitrary<List<Franchise>> distinctFranchiseLists() {
        return Arbitraries.integers().between(1, 5).flatMap(n ->
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(8)
                        .list().ofSize(n)
                        .map(names -> names.stream().map(name -> {
                            String fId = UUID.randomUUID().toString();
                            return Franchise.builder().id(fId).name(name).branches(List.of()).build();
                        }).toList()));
    }

    /**
     * Property 7: Repository save/find round-trip
     *
     * For any valid Franchise, findById(save(franchise).getId()) returns an Optional
     * containing a Franchise equal to the original.
     *
     * Feature: aws-dynamodb-infrastructure, Property 7: Repository save/find round-trip
     * Validates: Requirements 3.3
     */
    @Property(tries = 20)
    // Feature: aws-dynamodb-infrastructure, Property 7: Repository save/find round-trip
    void repositorySaveFindRoundTrip(@ForAll("validFranchises") Franchise franchise) {
        if (dockerUnavailable()) return; // skip gracefully when Docker is not available
        DynamoDbAsyncClient client = buildClient();
        createTable(client);
        try {
            FranchiseDynamoDbRepository repo = new FranchiseDynamoDbRepository(
                    client, new FranchiseMapper(), new BranchMapper(), new ProductMapper(), TABLE_NAME);
            repo.save(franchise);
            Optional<Franchise> found = repo.findById(franchise.getId());
            assertThat(found).isPresent();
            assertThat(found.get()).isEqualTo(franchise);
        } finally {
            client.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build()).join();
            client.close();
        }
    }

    /**
     * Property 8: findAll completeness
     *
     * For any collection of N distinct Franchise objects saved to the repository,
     * findAll() returns a list of size N containing all saved franchises.
     *
     * Feature: aws-dynamodb-infrastructure, Property 8: findAll completeness
     * Validates: Requirements 3.5
     */
    @Property(tries = 20)
    // Feature: aws-dynamodb-infrastructure, Property 8: findAll completeness
    void findAllCompleteness(@ForAll("distinctFranchiseLists") List<Franchise> franchises) {
        if (dockerUnavailable()) return; // skip gracefully when Docker is not available
        DynamoDbAsyncClient client = buildClient();
        createTable(client);
        try {
            FranchiseDynamoDbRepository repo = new FranchiseDynamoDbRepository(
                    client, new FranchiseMapper(), new BranchMapper(), new ProductMapper(), TABLE_NAME);
            franchises.forEach(repo::save);
            List<Franchise> all = repo.findAll();
            assertThat(all).hasSize(franchises.size());
        } finally {
            client.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build()).join();
            client.close();
        }
    }
}
