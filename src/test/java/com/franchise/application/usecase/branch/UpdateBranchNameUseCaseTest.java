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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateBranchNameUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @Mock
    private FindBranchLocationUseCase findBranchLocationUseCase;

    @InjectMocks
    private UpdateBranchNameUseCase useCase;

    @Test
    void givenExistingBranchWhenExecuteThenUpdateBranchName() {
        Branch branch = Branch.builder()
                .id("br-1")
                .name("Sucursal Vieja")
                .products(List.of())
                .build();
        Franchise franchise = Franchise.builder()
                .id("fr-1")
                .name("Franquicia")
                .branches(List.of(branch))
                .build();
        UpdateBranchNameUseCase.UpdateBranchNameRequest request =
                new UpdateBranchNameUseCase.UpdateBranchNameRequest("br-1", "Sucursal Nueva");

        when(findBranchLocationUseCase.execute(new FindBranchLocationUseCase.FindBranchLocationRequest("br-1")))
                .thenReturn(Mono.just(new FindBranchLocationUseCase.BranchLocation("fr-1", "br-1")));
        when(franchiseRepository.findById("fr-1")).thenReturn(Optional.of(franchise));
        when(franchiseRepository.save(org.mockito.ArgumentMatchers.any(Franchise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(useCase.execute(request))
                .assertNext(updatedFranchise -> {
                    Branch updatedBranch = updatedFranchise.getBranches().getFirst();
                    assertThat(updatedBranch.getName()).isEqualTo("Sucursal Nueva");
                    assertThat(updatedBranch.getId()).isEqualTo("br-1");
                })
                .verifyComplete();

        verify(franchiseRepository).findById("fr-1");
        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(captor.capture());
        assertThat(captor.getValue().getBranches().getFirst().getName()).isEqualTo("Sucursal Nueva");
    }

    @Test
    void givenMissingBranchWhenExecuteThenReturnErrorAndDoNotSave() {
        UpdateBranchNameUseCase.UpdateBranchNameRequest request =
                new UpdateBranchNameUseCase.UpdateBranchNameRequest("missing-branch", "Sucursal Nueva");

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
