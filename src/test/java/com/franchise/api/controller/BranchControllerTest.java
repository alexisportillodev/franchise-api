package com.franchise.api.controller;

import com.franchise.api.handler.ApiExceptionHandler;
import com.franchise.application.usecase.branch.UpdateBranchNameUseCase;
import com.franchise.application.usecase.product.AddProductToBranchUseCase;
import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
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

@WebFluxTest(controllers = BranchController.class)
@Import(ApiExceptionHandler.class)
class BranchControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UpdateBranchNameUseCase updateBranchNameUseCase;

    @MockitoBean
    private AddProductToBranchUseCase addProductToBranchUseCase;

    @Test
    @DisplayName("POST /branches/{id}/products should create a product")
    void shouldAddProductToBranch() {
        Product existing = Product.builder()
                .id("pr-1")
                .name("Mouse")
                .stock(5)
                .build();
        Product added = Product.builder()
                .id("pr-2")
                .name("Laptop")
                .stock(15)
                .build();
        Branch branch = Branch.builder()
                .id("br-1")
                .name("Sucursal Norte")
                .products(List.of(existing, added))
                .build();
        Franchise franchise = Franchise.builder()
                .id("fr-1")
                .name("Franquicia")
                .branches(List.of(branch))
                .build();

        when(addProductToBranchUseCase.execute(any()))
                .thenReturn(Mono.just(franchise));

        webTestClient.post()
                .uri("/branches/{id}/products", "br-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Laptop","stock":15}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo("pr-2")
                .jsonPath("$.name").isEqualTo("Laptop")
                .jsonPath("$.stock").isEqualTo(15);
    }

    @Test
    @DisplayName("PUT /branches/{id} should update the branch name")
    void shouldUpdateBranchName() {
        Branch branch = Branch.builder()
                .id("br-1")
                .name("Sucursal Renombrada")
                .products(List.of())
                .build();
        Franchise franchise = Franchise.builder()
                .id("fr-1")
                .name("Franquicia")
                .branches(List.of(branch))
                .build();

        when(updateBranchNameUseCase.execute(any())).thenReturn(Mono.just(franchise));

        webTestClient.put()
                .uri("/branches/{id}", "br-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Sucursal Renombrada"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo("br-1")
                .jsonPath("$.name").isEqualTo("Sucursal Renombrada");
    }

    @Test
    @DisplayName("POST /branches/{id}/products should return 400 for invalid payload")
    void shouldReturnBadRequestWhenProductPayloadIsInvalid() {
        webTestClient.post()
                .uri("/branches/{id}/products", "br-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"","stock":-1}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").exists();
    }

    @Test
    @DisplayName("PUT /branches/{id} should return 404 when branch does not exist")
    void shouldReturnNotFoundWhenUpdatingMissingBranch() {
        when(updateBranchNameUseCase.execute(any()))
                .thenReturn(Mono.error(new IllegalArgumentException("Branch not found")));

        webTestClient.put()
                .uri("/branches/{id}", "missing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Sucursal Renombrada"}
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Branch not found");
    }
}
