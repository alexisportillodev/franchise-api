package com.franchise.infrastructure.persistence.dynamodb.mapper;

import com.franchise.domain.model.Product;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.productPk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.productSk;

/**
 * Pure, stateless mapper between {@link Product} domain objects and
 * DynamoDB {@code Map<String, AttributeValue>} items.
 */
@Component
public class ProductMapper {

    static final String ATTR_PK        = "PK";
    static final String ATTR_SK        = "SK";
    static final String ATTR_ID        = "id";
    static final String ATTR_NAME      = "name";
    static final String ATTR_STOCK     = "stock";
    static final String ATTR_TYPE      = "entityType";
    static final String ATTR_BRANCH_ID = "branchId";

    /**
     * Converts a {@link Product} domain object to a DynamoDB attribute map.
     *
     * @param branchId the parent branch ID (used to build PK and stored as {@code branchId})
     * @param product  the product to serialise
     * @return a mutable map of DynamoDB attribute values
     */
    public Map<String, AttributeValue> toItem(String branchId, Product product) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(ATTR_PK,        AttributeValue.builder().s(productPk(branchId)).build());
        item.put(ATTR_SK,        AttributeValue.builder().s(productSk(product.getId())).build());
        item.put(ATTR_ID,        AttributeValue.builder().s(product.getId()).build());
        item.put(ATTR_NAME,      AttributeValue.builder().s(product.getName()).build());
        item.put(ATTR_STOCK,     AttributeValue.builder().n(String.valueOf(product.getStock())).build());
        item.put(ATTR_TYPE,      AttributeValue.builder().s("PRODUCT").build());
        item.put(ATTR_BRANCH_ID, AttributeValue.builder().s(branchId).build());
        return item;
    }

    /**
     * Reconstructs a {@link Product} domain object from a DynamoDB attribute map.
     *
     * @param item the DynamoDB item map
     * @return the reconstructed {@link Product}
     */
    public Product fromItem(Map<String, AttributeValue> item) {
        return Product.builder()
                .id(item.get(ATTR_ID).s())
                .name(item.get(ATTR_NAME).s())
                .stock(Integer.parseInt(item.get(ATTR_STOCK).n()))
                .build();
    }
}
