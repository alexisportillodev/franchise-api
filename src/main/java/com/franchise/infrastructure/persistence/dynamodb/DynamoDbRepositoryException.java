package com.franchise.infrastructure.persistence.dynamodb;

/**
 * Runtime exception thrown when a DynamoDB operation fails.
 * Wraps the underlying SDK exception so the domain and application layers
 * remain unaware of DynamoDB-specific error types.
 *
 * <p>This is a placeholder created in Task 6.1; the canonical definition
 * will be finalised in Task 8.1.</p>
 */
public class DynamoDbRepositoryException extends RuntimeException {

    public DynamoDbRepositoryException(String message) {
        super(message);
    }

    public DynamoDbRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
