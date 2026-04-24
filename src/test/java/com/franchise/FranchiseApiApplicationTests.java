package com.franchise;

import com.franchise.infrastructure.persistence.dynamodb.repository.FranchiseDynamoDbRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@SpringBootTest
class FranchiseApiApplicationTests {

	@MockitoBean
	DynamoDbAsyncClient dynamoDbAsyncClient;

	@MockitoBean
	FranchiseDynamoDbRepository franchiseDynamoDbRepository;

	@Test
	void contextLoads() {
	}

}
