package com.franchise.application.usecase.branch;

import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.port.in.FranchiseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddBranchToFranchiseUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private AddBranchToFranchiseUseCase useCase;

    @Test
    void givenExistingFranchiseWhenExecuteThenAddBranch() {
        Branch existingBranch = Branch.builder()
                .id("br-existing")
                .name("Sucursal Centro")
                .products(List.of())
                .build();
        Franchise franchise = Franchise.builder()
                .id("fr-1")
                .name("Franquicia")
                .branches(List.of(existingBranch))
                .build();
        AddBranchToFranchiseUseCase.AddBranchToFranchiseRequest request =
                new AddBranchToFranchiseUseCase.AddBranchToFranchiseRequest("fr-1", "Sucursal Norte");

        when(franchiseRepository.findById("fr-1")).thenReturn(Optional.of(franchise));
        when(franchiseRepository.save(org.mockito.ArgumentMatchers.any(Franchise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(useCase.execute(request))
                .assertNext(updatedFranchise -> {
                    assertThat(updatedFranchise.getBranches()).hasSize(2);
                    assertThat(updatedFranchise.getBranches().getFirst().getId()).isEqualTo("br-existing");
                    Branch branch = updatedFranchise.getBranches().getLast();
                    assertThat(branch.getName()).isEqualTo("Sucursal Norte");
                    assertThat(branch.getProducts()).isEmpty();
                    assertThat(branch.getId()).isNotBlank();
                })
                .verifyComplete();

        verify(franchiseRepository).findById("fr-1");
        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(captor.capture());
        assertThat(captor.getValue().getBranches()).hasSize(2);
        assertThat(captor.getValue().getBranches().getFirst().getId()).isEqualTo("br-existing");
        assertThat(captor.getValue().getBranches().getLast().getName()).isEqualTo("Sucursal Norte");
    }

    @Test
    void givenMissingFranchiseWhenExecuteThenReturnErrorAndDoNotSave() {
        AddBranchToFranchiseUseCase.AddBranchToFranchiseRequest request =
                new AddBranchToFranchiseUseCase.AddBranchToFranchiseRequest("missing", "Sucursal Norte");

        when(franchiseRepository.findById("missing")).thenReturn(Optional.empty());

        StepVerifier.create(useCase.execute(request))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class);
                    assertThat(error).hasMessage("Franchise not found");
                })
                .verify();

        verify(franchiseRepository).findById("missing");
        verify(franchiseRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
