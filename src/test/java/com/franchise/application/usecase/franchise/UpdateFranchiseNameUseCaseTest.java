package com.franchise.application.usecase.franchise;

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
class UpdateFranchiseNameUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private UpdateFranchiseNameUseCase useCase;

    @Test
    void givenExistingFranchiseWhenExecuteThenUpdateName() {
        Franchise existingFranchise = Franchise.builder()
                .id("fr-1")
                .name("Original")
                .branches(List.of())
                .build();
        UpdateFranchiseNameUseCase.UpdateFranchiseNameRequest request =
                new UpdateFranchiseNameUseCase.UpdateFranchiseNameRequest("fr-1", "Nuevo Nombre");

        when(franchiseRepository.findById("fr-1")).thenReturn(Optional.of(existingFranchise));
        when(franchiseRepository.save(org.mockito.ArgumentMatchers.any(Franchise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(useCase.execute(request))
                .assertNext(updatedFranchise -> {
                    assertThat(updatedFranchise.getId()).isEqualTo("fr-1");
                    assertThat(updatedFranchise.getName()).isEqualTo("Nuevo Nombre");
                    assertThat(updatedFranchise.getBranches()).isEmpty();
                })
                .verifyComplete();

        verify(franchiseRepository).findById("fr-1");
        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Nuevo Nombre");
    }

    @Test
    void givenMissingFranchiseWhenExecuteThenReturnErrorAndDoNotSave() {
        UpdateFranchiseNameUseCase.UpdateFranchiseNameRequest request =
                new UpdateFranchiseNameUseCase.UpdateFranchiseNameRequest("missing", "Nuevo Nombre");

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
