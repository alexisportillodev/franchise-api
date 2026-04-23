package com.franchise.application.usecase.product;

import com.franchise.application.usecase.branch.FindBranchLocationUseCase;
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
class AddProductToBranchUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @Mock
    private FindBranchLocationUseCase findBranchLocationUseCase;

    @InjectMocks
    private AddProductToBranchUseCase useCase;

    @Test
    void givenExistingBranchWhenExecuteThenAddProduct() {
        Branch branch = Branch.builder()
                .id("br-1")
                .name("Sucursal Norte")
                .products(List.of())
                .build();
        Franchise franchise = Franchise.builder()
                .id("fr-1")
                .name("Franquicia")
                .branches(List.of(branch))
                .build();
        AddProductToBranchUseCase.AddProductToBranchRequest request =
                new AddProductToBranchUseCase.AddProductToBranchRequest("br-1", "Laptop", 15);

        when(findBranchLocationUseCase.execute(new FindBranchLocationUseCase.FindBranchLocationRequest("br-1")))
                .thenReturn(Mono.just(new FindBranchLocationUseCase.BranchLocation("fr-1", "br-1")));
        when(franchiseRepository.findById("fr-1")).thenReturn(Optional.of(franchise));
        when(franchiseRepository.save(org.mockito.ArgumentMatchers.any(Franchise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(useCase.execute(request))
                .assertNext(updatedFranchise -> {
                    Product product = updatedFranchise.getBranches().getFirst().getProducts().getFirst();
                    assertThat(product.getName()).isEqualTo("Laptop");
                    assertThat(product.getStock()).isEqualTo(15);
                    assertThat(product.getId()).isNotBlank();
                })
                .verifyComplete();

        verify(franchiseRepository).findById("fr-1");
        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(captor.capture());
        assertThat(captor.getValue().getBranches().getFirst().getProducts()).hasSize(1);
    }

    @Test
    void givenMissingBranchWhenExecuteThenReturnErrorAndDoNotSave() {
        AddProductToBranchUseCase.AddProductToBranchRequest request =
                new AddProductToBranchUseCase.AddProductToBranchRequest("missing-branch", "Laptop", 15);

        when(findBranchLocationUseCase.execute(new FindBranchLocationUseCase.FindBranchLocationRequest("missing-branch")))
                .thenReturn(Mono.error(new IllegalArgumentException("Branch not found")));

        StepVerifier.create(useCase.execute(request))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class);
                    assertThat(error).hasMessage("Branch not found");
                })
                .verify();

        verify(findBranchLocationUseCase).execute(new FindBranchLocationUseCase.FindBranchLocationRequest("missing-branch"));
        verify(franchiseRepository, never()).findById(org.mockito.ArgumentMatchers.anyString());
        verify(franchiseRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
