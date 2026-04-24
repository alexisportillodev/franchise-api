package com.franchise.application.usecase.product;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.domain.port.in.FranchiseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveProductFromBranchUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @Mock
    private FindProductLocationUseCase findProductLocationUseCase;

    @InjectMocks
    private RemoveProductFromBranchUseCase useCase;

    @Test
    void givenExistingProductWhenExecuteThenRemoveItFromBranch() {
        Product productToRemove = Product.builder()
                .id("pr-1")
                .name("Laptop")
                .stock(10)
                .build();
        Product productToKeep = Product.builder()
                .id("pr-2")
                .name("Mouse")
                .stock(5)
                .build();
        Branch branch = Branch.builder()
                .id("br-1")
                .name("Sucursal")
                .products(List.of(productToRemove, productToKeep))
                .build();
        Franchise franchise = Franchise.builder()
                .id("fr-1")
                .name("Franquicia")
                .branches(List.of(branch))
                .build();
        RemoveProductFromBranchUseCase.RemoveProductFromBranchRequest request =
                new RemoveProductFromBranchUseCase.RemoveProductFromBranchRequest("pr-1");

        when(findProductLocationUseCase.execute(new FindProductLocationUseCase.FindProductLocationRequest("pr-1")))
                .thenReturn(Mono.just(new FindProductLocationUseCase.ProductLocation("fr-1", "br-1", "pr-1")));
        when(franchiseRepository.findById("fr-1")).thenReturn(Optional.of(franchise));
        when(franchiseRepository.save(org.mockito.ArgumentMatchers.any(Franchise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(useCase.execute(request))
                .assertNext(updatedFranchise -> {
                    List<Product> updatedProducts = updatedFranchise.getBranches().getFirst().getProducts();
                    assertThat(updatedProducts).hasSize(1);
                    assertThat(updatedProducts.getFirst().getId()).isEqualTo("pr-2");
                })
                .verifyComplete();

        verify(franchiseRepository).findById("fr-1");
        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(captor.capture());
        assertThat(captor.getValue().getBranches().getFirst().getProducts())
                .extracting(Product::getId)
                .containsExactly("pr-2");
    }

    @Test
    void givenMissingProductWhenExecuteThenReturnErrorAndDoNotSave() {
        RemoveProductFromBranchUseCase.RemoveProductFromBranchRequest request =
                new RemoveProductFromBranchUseCase.RemoveProductFromBranchRequest("missing-product");

        when(findProductLocationUseCase.execute(new FindProductLocationUseCase.FindProductLocationRequest("missing-product")))
                .thenReturn(Mono.error(new IllegalArgumentException("Product not found")));

        StepVerifier.create(useCase.execute(request))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class);
                    assertThat(error).hasMessage("Product not found");
                })
                .verify();

        verify(findProductLocationUseCase).execute(new FindProductLocationUseCase.FindProductLocationRequest("missing-product"));
        verify(franchiseRepository, never()).findById(org.mockito.ArgumentMatchers.anyString());
        verify(franchiseRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
