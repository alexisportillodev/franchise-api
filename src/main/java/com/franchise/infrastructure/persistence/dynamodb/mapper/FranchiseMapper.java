package com.franchise.infrastructure.persistence.dynamodb.mapper;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.franchisePk;

/**
 * Pure, stateless mapper between {@link Franchise} domain objects and
 * DynamoDB {@code Map<String, AttributeValue>} items.
 *
 * <p>Franchise items store franchise metadata only — nested branches and products
 * are stored as separate DynamoDB items and are passed in during reconstruction.</p>
 */
@Component
public class FranchiseMapper {

    static final String ATTR_PK   = "PK";
    static final String ATTR_SK   = "SK";
    static final String ATTR_ID   = "id";
    static final String ATTR_NAME = "name";
    static final String ATTR_TYPE = "entityType";

    /**
     * Converts a {@link Franchise} domain object to a DynamoDB attribute map.
     *
     * @param franchise the franchise to serialise
     * @return a mutable map of DynamoDB attribute values
     */
    public Map<String, AttributeValue> toItem(Franchise franchise) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(ATTR_PK,   AttributeValue.builder().s(franchisePk(franchise.getId())).build());
        item.put(ATTR_SK,   AttributeValue.builder().s(franchisePk(franchise.getId())).build());
        item.put(ATTR_ID,   AttributeValue.builder().s(franchise.getId()).build());
        item.put(ATTR_NAME, AttributeValue.builder().s(franchise.getName()).build());
        item.put(ATTR_TYPE, AttributeValue.builder().s("FRANCHISE").build());
        return item;
    }

    /**
     * Reconstructs a {@link Franchise} domain object from a DynamoDB attribute map and
     * a pre-fetched list of its branches (each already containing their products).
     *
     * @param item     the DynamoDB item map for the franchise
     * @param branches the branches belonging to this franchise
     * @return the reconstructed {@link Franchise}
     */
    public Franchise fromItem(Map<String, AttributeValue> item, List<Branch> branches) {
        return Franchise.builder()
                .id(item.get(ATTR_ID).s())
                .name(item.get(ATTR_NAME).s())
                .branches(branches)
                .build();
    }
}
