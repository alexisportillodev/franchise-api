package com.franchise.application.usecase.query;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.FranchiseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTopStockProductPerBranchUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private GetTopStockProductPerBranchUseCase useCase;

    @Test
    void givenFranchiseWithBranchesWhenExecuteThenReturnTopProductPerBranch() {
        Branch northBranch = Branch.builder()
                .id("br-1")
                .name("Norte")
                .products(List.of(
                        Product.builder().id("pr-1").name("Laptop").stock(10).build(),
                        Product.builder().id("pr-2").name("Tablet").stock(25).build()
                ))
                .build();
        Branch southBranch = Branch.builder()
                .id("br-2")
                .name("Sur")
                .products(List.of())
                .build();
        Franchise franchise = Franchise.builder()
                .id("fr-1")
                .name("Franquicia")
                .branches(List.of(northBranch, southBranch))
                .build();

        when(franchiseRepository.findById("fr-1")).thenReturn(Optional.of(franchise));

        StepVerifier.create(useCase.execute(new GetTopStockProductPerBranchUseCase.GetTopStockProductPerBranchRequest("fr-1")))
                .assertNext(topProduct -> {
                    assertThat(topProduct.branchName()).isEqualTo("Norte");
                    assertThat(topProduct.productName()).isEqualTo("Tablet");
                    assertThat(topProduct.stock()).isEqualTo(25);
                })
                .assertNext(topProduct -> {
                    assertThat(topProduct.branchName()).isEqualTo("Sur");
                    assertThat(topProduct.productName()).isNull();
                    assertThat(topProduct.stock()).isZero();
                })
                .verifyComplete();

        verify(franchiseRepository).findById("fr-1");
    }

    @Test
    void givenFranchiseWithoutBranchesWhenExecuteThenReturnEmptyFlux() {
        Franchise franchise = Franchise.builder()
                .id("fr-1")
                .name("Franquicia")
                .branches(List.of())
                .build();

        when(franchiseRepository.findById("fr-1")).thenReturn(Optional.of(franchise));

        StepVerifier.create(useCase.execute(new GetTopStockProductPerBranchUseCase.GetTopStockProductPerBranchRequest("fr-1")))
                .verifyComplete();

        verify(franchiseRepository).findById("fr-1");
    }

    @Test
    void givenMissingFranchiseWhenExecuteThenReturnError() {
        when(franchiseRepository.findById("missing")).thenReturn(Optional.empty());

        StepVerifier.create(useCase.execute(new GetTopStockProductPerBranchUseCase.GetTopStockProductPerBranchRequest("missing")))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class);
                    assertThat(error).hasMessage("Franchise not found");
                })
                .verify();

        verify(franchiseRepository).findById("missing");
    }
}
