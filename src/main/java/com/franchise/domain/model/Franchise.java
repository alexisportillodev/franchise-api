package com.franchise.domain.model;

import lombok.Builder;
import lombok.Value;
import java.util.List;

@Value
@Builder
public class Franchise {
    String id;
    String name;
    List<Branch> branches;
}
