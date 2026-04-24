package com.franchise.api.mapper;

import com.franchise.api.dto.response.TopProductResponse;
import com.franchise.application.usecase.query.GetTopStockProductPerBranchUseCase.TopProduct;

public class TopProductMapper {

    public static TopProductResponse toResponse(TopProduct topProduct) {
        return new TopProductResponse(
                topProduct.branchName(),
                topProduct.productName(),
                topProduct.stock()
        );
    }
}
