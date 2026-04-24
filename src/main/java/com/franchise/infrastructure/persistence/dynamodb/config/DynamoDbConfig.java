package com.franchise.infrastructure.persistence.dynamodb.config;

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

    @Value("${aws.region:us-east-2}")
    private String awsRegion;

    @Value("${aws.endpoint-override:}")
    private String endpointOverride;

    @Value("${aws.dynamodb.table-name:franchise}")
    private String tableName;

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {

        System.out.println(">>> [DYNAMO CONFIG] INIT CLIENT");
        System.out.println(">>> REGION: " + awsRegion);
        System.out.println(">>> ENDPOINT OVERRIDE: " + endpointOverride);

        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder()
                .region(Region.of(awsRegion));

        if (StringUtils.hasText(endpointOverride)) {
            System.out.println(">>> USING CUSTOM ENDPOINT: " + endpointOverride);
            builder.endpointOverride(URI.create(endpointOverride));
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(DynamoDbAsyncClient client) {
        System.out.println(">>> [DYNAMO CONFIG] ENHANCED CLIENT CREATED");
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(client)
                .build();
    }

    @Bean
    public String dynamoDbTableName() {
        System.out.println(">>> [DYNAMO CONFIG] TABLE NAME BEAN = " + tableName);
        return tableName;
    }
}
