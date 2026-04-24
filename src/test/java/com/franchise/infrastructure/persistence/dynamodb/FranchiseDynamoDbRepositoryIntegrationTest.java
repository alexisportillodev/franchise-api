package com.franchise.infrastructure.persistence.dynamodb;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.FranchiseRepository;
import com.franchise.infrastructure.persistence.dynamodb.mapper.BranchMapper;
import com.franchise.infrastructure.persistence.dynamodb.mapper.FranchiseMapper;
import com.franchise.infrastructure.persistence.dynamodb.mapper.ProductMapper;
import com.franchise.infrastructure.persistence.dynamodb.repository.FranchiseDynamoDbRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link FranchiseDynamoDbRepository} against a real
 * DynamoDB Local instance provided by Testcontainers LocalStack.
 *
 * These tests are skipped automatically when Docker is not available.
 *
 * Requirements: 3.2, 3.3, 3.5, 3.6, 3.8, 3.10
 */
class FranchiseDynamoDbRepositoryIntegrationTest {

    private static final String TABLE_NAME = "franchise-table";
    private static LocalStackContainer localStack;

    private DynamoDbAsyncClient dynamoDbClient;
    private FranchiseDynamoDbRepository repository;

    @BeforeAll
    static void checkDocker() {
        assumeTrue(isDockerAvailable(), "Docker is not available — skipping integration tests");
        localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
                .withServices(LocalStackContainer.Service.DYNAMODB);
        localStack.start();
    }

    private static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeEach
    void setUp() {
        dynamoDbClient = DynamoDbAsyncClient.builder()
                .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .region(Region.of(localStack.getRegion()))
                .build();
        createTable();
        repository = new FranchiseDynamoDbRepository(
                dynamoDbClient, new FranchiseMapper(), new BranchMapper(), new ProductMapper(), TABLE_NAME);
    }

    @AfterEach
    void tearDown() {
        dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build()).join();
        dynamoDbClient.close();
    }

    private void createTable() {
        dynamoDbClient.createTable(CreateTableRequest.builder()
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

    private Franchise buildFranchise(String fId, String bId, String pId) {
        Product product = Product.builder().id(pId).name("Product-" + pId).stock(10).build();
        Branch branch = Branch.builder().id(bId).name("Branch-" + bId).products(List.of(product)).build();
        return Franchise.builder().id(fId).name("Franchise-" + fId).branches(List.of(branch)).build();
    }

    /** Requirements: 3.2, 3.3 */
    @Test
    void save_and_findById_roundTrip() {
        Franchise franchise = buildFranchise("f1", "b1", "p1");
        repository.save(franchise);
        Optional<Franchise> found = repository.findById("f1");
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(franchise);
    }

    /** Requirements: 3.5 */
    @Test
    void findAll_returnsAllSavedFranchises() {
        repository.save(buildFranchise("f1", "b1", "p1"));
        repository.save(buildFranchise("f2", "b2", "p2"));
        repository.save(buildFranchise("f3", "b3", "p3"));
        List<Franchise> all = repository.findAll();
        assertThat(all).hasSize(3);
        assertThat(all).extracting(Franchise::getId).containsExactlyInAnyOrder("f1", "f2", "f3");
    }

    /** Requirements: 3.6 */
    @Test
    void deleteById_removesAllItems() {
        repository.save(buildFranchise("f1", "b1", "p1"));
        repository.deleteById("f1");
        assertThat(repository.findById("f1")).isEmpty();
    }

    /** Requirements: 3.8 */
    @Test
    void findBranchLocation_returnsCorrectLocation() {
        repository.save(buildFranchise("f1", "b1", "p1"));
        Optional<FranchiseRepository.BranchLocation> loc = repository.findBranchLocation("b1");
        assertThat(loc).isPresent();
        assertThat(loc.get().franchiseId()).isEqualTo("f1");
        assertThat(loc.get().branchId()).isEqualTo("b1");
    }

    /** Requirements: 3.10 */
    @Test
    void findProductLocation_returnsCorrectLocation() {
        repository.save(buildFranchise("f1", "b1", "p1"));
        Optional<FranchiseRepository.ProductLocation> loc = repository.findProductLocation("p1");
        assertThat(loc).isPresent();
        assertThat(loc.get().franchiseId()).isEqualTo("f1");
        assertThat(loc.get().branchId()).isEqualTo("b1");
        assertThat(loc.get().productId()).isEqualTo("p1");
    }
}
