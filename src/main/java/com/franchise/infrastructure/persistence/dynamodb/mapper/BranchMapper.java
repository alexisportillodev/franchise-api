package com.franchise.infrastructure.persistence.dynamodb.mapper;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Product;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.branchSk;
import static com.franchise.infrastructure.persistence.dynamodb.mapper.DynamoDbKeys.franchisePk;

/**
 * Pure, stateless mapper between {@link Branch} domain objects and
 * DynamoDB {@code Map<String, AttributeValue>} items.
 *
 * <p>Branch items store branch metadata only — nested products are stored as
 * separate DynamoDB items and are passed in during reconstruction.</p>
 */
@Component
public class BranchMapper {

    static final String ATTR_PK           = "PK";
    static final String ATTR_SK           = "SK";
    static final String ATTR_ID           = "id";
    static final String ATTR_NAME         = "name";
    static final String ATTR_TYPE         = "entityType";
    static final String ATTR_FRANCHISE_ID = "franchiseId";

    /**
     * Converts a {@link Branch} domain object to a DynamoDB attribute map.
     *
     * @param franchiseId the parent franchise ID (used to build PK and stored as {@code franchiseId})
     * @param branch      the branch to serialise
     * @return a mutable map of DynamoDB attribute values
     */
    public Map<String, AttributeValue> toItem(String franchiseId, Branch branch) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(ATTR_PK,           AttributeValue.builder().s(franchisePk(franchiseId)).build());
        item.put(ATTR_SK,           AttributeValue.builder().s(branchSk(branch.getId())).build());
        item.put(ATTR_ID,           AttributeValue.builder().s(branch.getId()).build());
        item.put(ATTR_NAME,         AttributeValue.builder().s(branch.getName()).build());
        item.put(ATTR_TYPE,         AttributeValue.builder().s("BRANCH").build());
        item.put(ATTR_FRANCHISE_ID, AttributeValue.builder().s(franchiseId).build());
        return item;
    }

    /**
     * Reconstructs a {@link Branch} domain object from a DynamoDB attribute map and
     * a pre-fetched list of its products.
     *
     * @param item     the DynamoDB item map for the branch
     * @param products the products belonging to this branch
     * @return the reconstructed {@link Branch}
     */
    public Branch fromItem(Map<String, AttributeValue> item, List<Product> products) {
        return Branch.builder()
                .id(item.get(ATTR_ID).s())
                .name(item.get(ATTR_NAME).s())
                .products(products)
                .build();
    }
}
