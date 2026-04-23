package com.franchise.api.controller;

import com.franchise.api.handler.ApiExceptionHandler;
import com.franchise.application.usecase.product.RemoveProductFromBranchUseCase;
import com.franchise.application.usecase.product.UpdateProductNameUseCase;
import com.franchise.application.usecase.product.UpdateProductStockUseCase;
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

@WebFluxTest(controllers = ProductController.class)
@Import(ApiExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RemoveProductFromBranchUseCase removeProductFromBranchUseCase;

    @MockitoBean
    private UpdateProductNameUseCase updateProductNameUseCase;

    @MockitoBean
    private UpdateProductStockUseCase updateProductStockUseCase;

    @Test
    @DisplayName("PUT /products/{id}/stock should update product stock")
    void shouldUpdateProductStock() {
        Franchise franchise = franchiseWithProduct("pr-1", "Laptop", 25);

        when(updateProductStockUseCase.execute(any()))
                .thenReturn(Mono.just(franchise));

        webTestClient.put()
                .uri("/products/{id}/stock", "pr-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"stock":25}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo("pr-1")
                .jsonPath("$.name").isEqualTo("Laptop")
                .jsonPath("$.stock").isEqualTo(25);
    }

    @Test
    @DisplayName("PUT /products/{id}/name should update product name")
    void shouldUpdateProductName() {
        Franchise franchise = franchiseWithProduct("pr-1", "Laptop Pro", 10);

        when(updateProductNameUseCase.execute(any()))
                .thenReturn(Mono.just(franchise));

        webTestClient.put()
                .uri("/products/{id}/name", "pr-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"Laptop Pro"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo("pr-1")
                .jsonPath("$.name").isEqualTo("Laptop Pro")
                .jsonPath("$.stock").isEqualTo(10);
    }

    @Test
    @DisplayName("DELETE /products/{id} should return the removed product projection")
    void shouldRemoveProduct() {
        Franchise franchise = franchiseWithProduct("pr-1", "Laptop", 10);

        when(removeProductFromBranchUseCase.execute(any()))
                .thenReturn(Mono.just(franchise));

        webTestClient.delete()
                .uri("/products/{id}", "pr-1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo("pr-1")
                .jsonPath("$.name").isEqualTo("Laptop")
                .jsonPath("$.stock").isEqualTo(10);
    }

    @Test
    @DisplayName("PUT /products/{id}/stock should return 400 for invalid payload")
    void shouldReturnBadRequestWhenUpdatingStockWithInvalidPayload() {
        webTestClient.put()
                .uri("/products/{id}/stock", "pr-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"stock":-1}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Stock must be non-negative");
    }

    @Test
    @DisplayName("PUT /products/{id}/name should return 400 for invalid payload")
    void shouldReturnBadRequestWhenUpdatingNameWithInvalidPayload() {
        webTestClient.put()
                .uri("/products/{id}/name", "pr-1")
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
    @DisplayName("DELETE /products/{id} should return 404 when product does not exist")
    void shouldReturnNotFoundWhenRemovingMissingProduct() {
        when(removeProductFromBranchUseCase.execute(any()))
                .thenReturn(Mono.error(new IllegalArgumentException("Product not found")));

        webTestClient.delete()
                .uri("/products/{id}", "missing")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Product not found");
    }

    private Franchise franchiseWithProduct(String productId, String productName, int stock) {
        Product product = Product.builder()
                .id(productId)
                .name(productName)
                .stock(stock)
                .build();
        Branch branch = Branch.builder()
                .id("br-1")
                .name("Sucursal Centro")
                .products(List.of(product))
                .build();
        return Franchise.builder()
                .id("fr-1")
                .name("Franquicia")
                .branches(List.of(branch))
                .build();
    }
}
