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
class UpdateProductNameUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @Mock
    private FindProductLocationUseCase findProductLocationUseCase;

    @InjectMocks
    private UpdateProductNameUseCase useCase;

    @Test
    void givenExistingProductWhenExecuteThenUpdateName() {
        Product product = Product.builder()
                .id("pr-1")
                .name("Laptop Vieja")
                .stock(10)
                .build();
        Branch branch = Branch.builder()
                .id("br-1")
                .name("Sucursal")
                .products(List.of(product))
                .build();
        Franchise franchise = Franchise.builder()
                .id("fr-1")
                .name("Franquicia")
                .branches(List.of(branch))
                .build();
        UpdateProductNameUseCase.UpdateProductNameRequest request =
                new UpdateProductNameUseCase.UpdateProductNameRequest("pr-1", "Laptop Nueva");

        when(findProductLocationUseCase.execute(new FindProductLocationUseCase.FindProductLocationRequest("pr-1")))
                .thenReturn(Mono.just(new FindProductLocationUseCase.ProductLocation("fr-1", "br-1", "pr-1")));
        when(franchiseRepository.findById("fr-1")).thenReturn(Optional.of(franchise));
        when(franchiseRepository.save(org.mockito.ArgumentMatchers.any(Franchise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(useCase.execute(request))
                .assertNext(updatedFranchise -> {
                    Product updatedProduct = updatedFranchise.getBranches().getFirst().getProducts().getFirst();
                    assertThat(updatedProduct.getName()).isEqualTo("Laptop Nueva");
                    assertThat(updatedProduct.getStock()).isEqualTo(10);
                })
                .verifyComplete();

        verify(franchiseRepository).findById("fr-1");
        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(captor.capture());
        assertThat(captor.getValue().getBranches().getFirst().getProducts().getFirst().getName()).isEqualTo("Laptop Nueva");
    }

    @Test
    void givenMissingProductWhenExecuteThenReturnErrorAndDoNotSave() {
        UpdateProductNameUseCase.UpdateProductNameRequest request =
                new UpdateProductNameUseCase.UpdateProductNameRequest("missing-product", "Laptop Nueva");

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
