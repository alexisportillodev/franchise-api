package com.franchise.application.usecase.branch;

import com.franchise.domain.port.in.FranchiseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindBranchLocationUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private FindBranchLocationUseCase useCase;

    @Test
    void givenExistingBranchWhenExecuteThenReturnLocation() {
        when(franchiseRepository.findBranchLocation("br-1"))
                .thenReturn(Optional.of(new FranchiseRepository.BranchLocation("fr-1", "br-1")));

        Mono<FindBranchLocationUseCase.BranchLocation> result =
                useCase.execute(new FindBranchLocationUseCase.FindBranchLocationRequest("br-1"));

        StepVerifier.create(result)
                .assertNext(location -> {
                    assertThat(location.franchiseId()).isEqualTo("fr-1");
                    assertThat(location.branchId()).isEqualTo("br-1");
                })
                .verifyComplete();

        verify(franchiseRepository).findBranchLocation("br-1");
    }

    @Test
    void givenMissingBranchWhenExecuteThenReturnError() {
        when(franchiseRepository.findBranchLocation("missing-branch"))
                .thenReturn(Optional.empty());

        StepVerifier.create(useCase.execute(new FindBranchLocationUseCase.FindBranchLocationRequest("missing-branch")))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class);
                    assertThat(error).hasMessage("Branch not found");
                })
                .verify();

        verify(franchiseRepository).findBranchLocation("missing-branch");
    }
}
