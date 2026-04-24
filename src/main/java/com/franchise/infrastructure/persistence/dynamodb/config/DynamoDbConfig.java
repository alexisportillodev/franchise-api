package com.franchise.infrastructure.persistence.dynamodb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbConfig.class);

    @Value("${aws.region:us-east-2}")
    private String awsRegion;

    @Value("${aws.endpoint-override:}")
    private String endpointOverride;

    @Value("${aws.dynamodb.table-name:franchise}")
    private String tableName;

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        log.info("Initializing DynamoDB async client — region={}", awsRegion);

        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder()
                .region(Region.of(awsRegion));

        if (StringUtils.hasText(endpointOverride)) {
            log.info("Using custom DynamoDB endpoint: {}", endpointOverride);
            builder.endpointOverride(URI.create(endpointOverride));
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(DynamoDbAsyncClient client) {
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(client)
                .build();
    }

    @Bean
    public String dynamoDbTableName() {
        log.info("DynamoDB table name: {}", tableName);
        return tableName;
    }
}
