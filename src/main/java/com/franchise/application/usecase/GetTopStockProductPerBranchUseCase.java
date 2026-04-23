package com.franchise.application.usecase;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.FranchiseRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.Optional;

public class GetTopStockProductPerBranchUseCase {

    private final FranchiseRepository franchiseRepository;

    public GetTopStockProductPerBranchUseCase(FranchiseRepository franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Flux<TopProduct> execute() {
        return Mono.fromCallable(() -> franchiseRepository.findAll())
                .flatMapMany(Flux::fromIterable)
                .flatMap(franchise -> Flux.fromIterable(franchise.getBranches()))
                .map(this::getTopProductForBranch);
    }

    private TopProduct getTopProductForBranch(Branch branch) {
        Optional<Product> topProduct = branch.getProducts().stream()
                .max(Comparator.comparingInt(Product::getStock));
        return topProduct.map(product -> new TopProduct(branch.getName(), product.getName(), product.getStock()))
                .orElse(new TopProduct(branch.getName(), null, 0));
    }

    public record TopProduct(String branchName, String productName, int stock) {}
}