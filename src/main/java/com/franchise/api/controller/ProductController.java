package com.franchise.api.controller;

import com.franchise.api.dto.request.UpdateProductNameRequest;
import com.franchise.api.dto.request.UpdateProductStockRequest;
import com.franchise.api.dto.response.FranchiseResponse;
import com.franchise.api.mapper.FranchiseMapper;
import com.franchise.application.usecase.product.RemoveProductFromBranchUseCase;
import com.franchise.application.usecase.product.UpdateProductNameUseCase;
import com.franchise.application.usecase.product.UpdateProductStockUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final RemoveProductFromBranchUseCase removeProductFromBranchUseCase;
    private final UpdateProductNameUseCase updateProductNameUseCase;
    private final UpdateProductStockUseCase updateProductStockUseCase;

    public ProductController(RemoveProductFromBranchUseCase removeProductFromBranchUseCase,
                             UpdateProductNameUseCase updateProductNameUseCase,
                             UpdateProductStockUseCase updateProductStockUseCase) {
        this.removeProductFromBranchUseCase = removeProductFromBranchUseCase;
        this.updateProductNameUseCase = updateProductNameUseCase;
        this.updateProductStockUseCase = updateProductStockUseCase;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeProduct(@PathVariable String id) {
        return removeProductFromBranchUseCase.execute(
                new RemoveProductFromBranchUseCase.RemoveProductFromBranchRequest(id)
        ).then();
    }

    @PutMapping("/{id}/stock")
    public Mono<FranchiseResponse> updateProductStock(@PathVariable String id,
                                                      @Valid @RequestBody UpdateProductStockRequest request) {
        return updateProductStockUseCase.execute(
                new UpdateProductStockUseCase.UpdateProductStockRequest(id, request.stock())
        ).map(FranchiseMapper::toResponse);
    }

    @PutMapping("/{id}/name")
    public Mono<FranchiseResponse> updateProductName(@PathVariable String id,
                                                     @Valid @RequestBody UpdateProductNameRequest request) {
        return updateProductNameUseCase.execute(
                new UpdateProductNameUseCase.UpdateProductNameRequest(id, request.name())
        ).map(FranchiseMapper::toResponse);
    }
}
