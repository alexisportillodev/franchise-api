package com.franchise.application.usecase.query;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;

public class GetTopStockProductPerBranchUseCase {

    private final FranchiseRepository franchiseRepository;

    public GetTopStockProductPerBranchUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Flux<TopProduct> execute(GetTopStockProductPerBranchRequest request) {
        return Mono.fromSupplier(() -> franchiseRepository.findById(request.franchiseId())
                        .orElseThrow(() -> new IllegalArgumentException("Franchise not found")))
                .flatMapMany(franchise -> Flux.fromIterable(franchise.getBranches()))
                .map(this::getTopProductForBranch);
    }

    private TopProduct getTopProductForBranch(Branch branch) {
        return branch.getProducts().stream()
                .max(Comparator.comparingInt(Product::getStock))
                .map(product -> new TopProduct(branch.getName(), product.getName(), product.getStock()))
                .orElse(new TopProduct(branch.getName(), null, 0));
    }

    public record GetTopStockProductPerBranchRequest(String franchiseId) {}

    public record TopProduct(String branchName, String productName, int stock) {}
}
