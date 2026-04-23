package com.franchise.application.usecase.product;

import com.franchise.domain.port.in.FranchiseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindProductLocationUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private FindProductLocationUseCase useCase;

    @Test
    void givenExistingProductWhenExecuteThenReturnLocation() {
        when(franchiseRepository.findProductLocation("pr-1"))
                .thenReturn(Optional.of(new FranchiseRepository.ProductLocation("fr-1", "br-1", "pr-1")));

        StepVerifier.create(useCase.execute(new FindProductLocationUseCase.FindProductLocationRequest("pr-1")))
                .assertNext(location -> {
                    assertThat(location.franchiseId()).isEqualTo("fr-1");
                    assertThat(location.branchId()).isEqualTo("br-1");
                    assertThat(location.productId()).isEqualTo("pr-1");
                })
                .verifyComplete();

        verify(franchiseRepository).findProductLocation("pr-1");
    }

    @Test
    void givenMissingProductWhenExecuteThenReturnError() {
        when(franchiseRepository.findProductLocation("missing-product"))
                .thenReturn(Optional.empty());

        StepVerifier.create(useCase.execute(new FindProductLocationUseCase.FindProductLocationRequest("missing-product")))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class);
                    assertThat(error).hasMessage("Product not found");
                })
                .verify();

        verify(franchiseRepository).findProductLocation("missing-product");
    }
}
