package com.franchise.api.controller;

import com.franchise.api.dto.request.BranchRequest;
import com.franchise.api.dto.request.FranchiseRequest;
import com.franchise.api.dto.response.BranchResponse;
import com.franchise.api.dto.response.FranchiseResponse;
import com.franchise.api.dto.response.TopProductResponse;
import com.franchise.api.mapper.BranchMapper;
import com.franchise.api.mapper.FranchiseMapper;
import com.franchise.api.mapper.TopProductMapper;
import com.franchise.application.usecase.branch.AddBranchToFranchiseUseCase;
import com.franchise.application.usecase.franchise.CreateFranchiseUseCase;
import com.franchise.application.usecase.franchise.UpdateFranchiseNameUseCase;
import com.franchise.application.usecase.query.GetTopStockProductPerBranchUseCase;
import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/franchises")
public class FranchiseController {

    private final CreateFranchiseUseCase createFranchiseUseCase;
    private final UpdateFranchiseNameUseCase updateFranchiseNameUseCase;
    private final AddBranchToFranchiseUseCase addBranchToFranchiseUseCase;
    private final GetTopStockProductPerBranchUseCase getTopStockProductPerBranchUseCase;

    public FranchiseController(CreateFranchiseUseCase createFranchiseUseCase,
                               UpdateFranchiseNameUseCase updateFranchiseNameUseCase,
                               AddBranchToFranchiseUseCase addBranchToFranchiseUseCase,
                               GetTopStockProductPerBranchUseCase getTopStockProductPerBranchUseCase) {
        this.createFranchiseUseCase = createFranchiseUseCase;
        this.updateFranchiseNameUseCase = updateFranchiseNameUseCase;
        this.addBranchToFranchiseUseCase = addBranchToFranchiseUseCase;
        this.getTopStockProductPerBranchUseCase = getTopStockProductPerBranchUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<FranchiseResponse> createFranchise(@Valid @RequestBody FranchiseRequest request) {
        return createFranchiseUseCase.execute(new CreateFranchiseUseCase.CreateFranchiseRequest(request.name()))
                .map(FranchiseMapper::toResponse);
    }

    @PutMapping("/{id}")
    public Mono<FranchiseResponse> updateFranchiseName(@PathVariable String id,
                                                       @Valid @RequestBody FranchiseRequest request) {
        return updateFranchiseNameUseCase.execute(
                new UpdateFranchiseNameUseCase.UpdateFranchiseNameRequest(id, request.name())
        ).map(FranchiseMapper::toResponse);
    }

    @PostMapping("/{id}/branches")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BranchResponse> addBranchToFranchise(@PathVariable String id,
                                                     @Valid @RequestBody BranchRequest request) {
        return addBranchToFranchiseUseCase.execute(
                new AddBranchToFranchiseUseCase.AddBranchToFranchiseRequest(id, request.name())
        ).map(this::extractLastBranch)
         .map(BranchMapper::toResponse);
    }

    @GetMapping("/{id}/top-stock")
    public Flux<TopProductResponse> getTopStockProducts(@PathVariable String id) {
        return getTopStockProductPerBranchUseCase.execute(
                new GetTopStockProductPerBranchUseCase.GetTopStockProductPerBranchRequest(id)
        ).map(TopProductMapper::toResponse);
    }

    private Branch extractLastBranch(Franchise franchise) {
        return franchise.getBranches().stream()
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
    }
}
