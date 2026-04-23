package com.franchise.api.controller;

import com.franchise.api.dto.request.ProductRequest;
import com.franchise.api.dto.request.UpdateBranchNameRequest;
import com.franchise.api.dto.response.BranchResponse;
import com.franchise.api.dto.response.ProductResponse;
import com.franchise.api.mapper.BranchMapper;
import com.franchise.api.mapper.ProductMapper;
import com.franchise.application.usecase.branch.UpdateBranchNameUseCase;
import com.franchise.application.usecase.product.AddProductToBranchUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/branches")
public class BranchController {

    private final UpdateBranchNameUseCase updateBranchNameUseCase;
    private final AddProductToBranchUseCase addProductToBranchUseCase;

    public BranchController(UpdateBranchNameUseCase updateBranchNameUseCase,
                            AddProductToBranchUseCase addProductToBranchUseCase) {
        this.updateBranchNameUseCase = updateBranchNameUseCase;
        this.addProductToBranchUseCase = addProductToBranchUseCase;
    }

    @PutMapping("/{id}")
    public Mono<BranchResponse> updateBranchName(@PathVariable String id,
                                                 @Valid @RequestBody UpdateBranchNameRequest request) {
        return updateBranchNameUseCase.execute(
                new UpdateBranchNameUseCase.UpdateBranchNameRequest(id, request.name())
        ).map(franchise -> BranchMapper.toResponse(franchise, id));
    }

    @PostMapping("/{id}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductResponse> addProductToBranch(@PathVariable String id,
                                                    @Valid @RequestBody ProductRequest request) {
        return addProductToBranchUseCase.execute(
                new AddProductToBranchUseCase.AddProductToBranchRequest(id, request.name(), request.stock())
        ).map(franchise -> ProductMapper.toResponse(franchise, id, request.name(), request.stock()));
    }
}
