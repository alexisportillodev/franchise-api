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
        Franchise franchise = Franchise.builder()
                .id("fr-1")
                .name("Franquicia")
                .branches(List.of())
                .build();
        AddBranchToFranchiseUseCase.AddBranchToFranchiseRequest request =
                new AddBranchToFranchiseUseCase.AddBranchToFranchiseRequest("fr-1", "Sucursal Norte");

        when(franchiseRepository.findById("fr-1")).thenReturn(Optional.of(franchise));
        when(franchiseRepository.save(org.mockito.ArgumentMatchers.any(Franchise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(useCase.execute(request))
                .assertNext(updatedFranchise -> {
                    assertThat(updatedFranchise.getBranches()).hasSize(1);
                    Branch branch = updatedFranchise.getBranches().getFirst();
                    assertThat(branch.getName()).isEqualTo("Sucursal Norte");
                    assertThat(branch.getProducts()).isEmpty();
                    assertThat(branch.getId()).isNotBlank();
                })
                .verifyComplete();

        verify(franchiseRepository).findById("fr-1");
        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(captor.capture());
        assertThat(captor.getValue().getBranches()).hasSize(1);
        assertThat(captor.getValue().getBranches().getFirst().getName()).isEqualTo("Sucursal Norte");
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
