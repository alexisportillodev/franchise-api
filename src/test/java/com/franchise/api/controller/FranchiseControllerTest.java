package com.franchise.api.controller;

import com.franchise.api.handler.ApiExceptionHandler;
import com.franchise.application.usecase.branch.AddBranchToFranchiseUseCase;
import com.franchise.application.usecase.franchise.CreateFranchiseUseCase;
import com.franchise.application.usecase.franchise.UpdateFranchiseNameUseCase;
import com.franchise.application.usecase.query.GetTopStockProductPerBranchUseCase;
import com.franchise.domain.model.Franchise;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = FranchiseController.class)
@Import(ApiExceptionHandler.class)
class FranchiseControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreateFranchiseUseCase createFranchiseUseCase;

    @MockitoBean
    private UpdateFranchiseNameUseCase updateFranchiseNameUseCase;

    @MockitoBean
    private AddBranchToFranchiseUseCase addBranchToFranchiseUseCase;

    @MockitoBean
    private GetTopStockProductPerBranchUseCase getTopStockProductPerBranchUseCase;

    @Test
    @DisplayName("POST /franchises should create a franchise")
    void shouldCreateFranchise() {
        Franchise franchise = Franchise.builder()
                .id("fr-1")
                .name("Franquicia Test")
                .branches(List.of())
                .build();

        when(createFranchiseUseCase.execute(any())).thenReturn(Mono.just(franchise));

        webTestClient.post()
                .uri("/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Franquicia Test"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo("fr-1")
                .jsonPath("$.name").isEqualTo("Franquicia Test")
                .jsonPath("$.branches").isArray();
    }

    @Test
    @DisplayName("PUT /franchises/{id} should update the franchise name")
    void shouldUpdateFranchiseName() {
        Franchise franchise = Franchise.builder()
                .id("fr-1")
                .name("Franquicia Renombrada")
                .branches(List.of())
                .build();

        when(updateFranchiseNameUseCase.execute(any()))
                .thenReturn(Mono.just(franchise));

        webTestClient.put()
                .uri("/franchises/{id}", "fr-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Franquicia Renombrada"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo("fr-1")
                .jsonPath("$.name").isEqualTo("Franquicia Renombrada");
    }

    @Test
    @DisplayName("POST /franchises should return 400 for invalid payload")
    void shouldReturnBadRequestWhenCreateFranchisePayloadIsInvalid() {
        webTestClient.post()
                .uri("/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":""}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Name is required");
    }

    @Test
    @DisplayName("PUT /franchises/{id} should return 404 when franchise does not exist")
    void shouldReturnNotFoundWhenUpdatingMissingFranchise() {
        when(updateFranchiseNameUseCase.execute(any()))
                .thenReturn(Mono.error(new IllegalArgumentException("Franchise not found")));

        webTestClient.put()
                .uri("/franchises/{id}", "missing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Renombrada"}
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Franchise not found");
    }

    @Test
    @DisplayName("PUT /franchises/{id} should return 400 for invalid payload")
    void shouldReturnBadRequestWhenUpdatingFranchiseWithInvalidPayload() {
        webTestClient.put()
                .uri("/franchises/{id}", "fr-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":""}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Name is required");
    }
}
