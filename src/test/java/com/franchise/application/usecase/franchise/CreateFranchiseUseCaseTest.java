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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFranchiseUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private CreateFranchiseUseCase useCase;

    @Test
    void givenValidRequestWhenExecuteThenCreateFranchiseWithEmptyBranches() {
        CreateFranchiseUseCase.CreateFranchiseRequest request =
                new CreateFranchiseUseCase.CreateFranchiseRequest("Franquicia Centro");

        when(franchiseRepository.save(org.mockito.ArgumentMatchers.any(Franchise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(useCase.execute(request))
                .assertNext(franchise -> {
                    assertThat(franchise.getName()).isEqualTo("Franquicia Centro");
                    assertThat(franchise.getBranches()).isEmpty();
                    assertThat(franchise.getId()).isNotBlank();
                })
                .verifyComplete();

        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(captor.capture());

        Franchise savedFranchise = captor.getValue();
        assertThat(savedFranchise.getName()).isEqualTo("Franquicia Centro");
        assertThat(savedFranchise.getBranches()).isEmpty();
        assertThat(savedFranchise.getId()).isNotBlank();
    }
}
