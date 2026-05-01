package com.yas.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yas.cart.viewmodel.ProductThumbnailVm;
import com.yas.commonlibrary.config.ServiceUrlConfig;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class ProductServiceAdditionalTest {

    private RestClient restClient;
    private ServiceUrlConfig serviceUrlConfig;
    private ProductService productService;

    // Use raw types to avoid generic capture mismatches (same pattern as ProductServiceTest)
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        restClient = Mockito.mock(RestClient.class);
        serviceUrlConfig = Mockito.mock(ServiceUrlConfig.class);
        productService = new ProductService(restClient, serviceUrlConfig);
        requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
    }

    @SuppressWarnings("unchecked")
    private void mockGetProducts(List<Long> ids, List<ProductThumbnailVm> returned) {
        URI url = UriComponentsBuilder
                .fromUriString("http://product")
                .path("/storefront/products/list-featured")
                .queryParam("productId", ids)
                .build()
                .toUri();
        when(serviceUrlConfig.product()).thenReturn("http://product");
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(new ParameterizedTypeReference<List<ProductThumbnailVm>>() {}))
                .thenReturn(ResponseEntity.ok(returned));
    }

    @SuppressWarnings("unchecked")
    private void mockGetProductsNullBody(List<Long> ids) {
        URI url = UriComponentsBuilder
                .fromUriString("http://product")
                .path("/storefront/products/list-featured")
                .queryParam("productId", ids)
                .build()
                .toUri();
        when(serviceUrlConfig.product()).thenReturn("http://product");
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(new ParameterizedTypeReference<List<ProductThumbnailVm>>() {}))
                .thenReturn(ResponseEntity.ok(null));
    }

    // ── getProductById ────────────────────────────────────────────────────────

    @Nested
    class GetProductByIdTest {

        @Test
        void getProductById_whenProductExists_returnsFirst() {
            ProductThumbnailVm vm = new ProductThumbnailVm(1L, "Phone", "phone", "img.jpg");
            mockGetProducts(List.of(1L), List.of(vm));

            ProductThumbnailVm result = productService.getProductById(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        void getProductById_whenListEmpty_returnsNull() {
            mockGetProducts(List.of(99L), List.of());

            ProductThumbnailVm result = productService.getProductById(99L);

            assertThat(result).isNull();
        }

        @Test
        void getProductById_whenResponseBodyNull_returnsNull() {
            mockGetProductsNullBody(List.of(5L));

            ProductThumbnailVm result = productService.getProductById(5L);

            assertThat(result).isNull();
        }
    }

    // ── existsById ────────────────────────────────────────────────────────────

    @Nested
    class ExistsByIdTest {

        @Test
        void existsById_whenProductFound_returnsTrue() {
            ProductThumbnailVm vm = new ProductThumbnailVm(1L, "Phone", "phone", "img.jpg");
            mockGetProducts(List.of(1L), List.of(vm));

            assertThat(productService.existsById(1L)).isTrue();
        }

        @Test
        void existsById_whenProductNotFound_returnsFalse() {
            mockGetProducts(List.of(99L), List.of());

            assertThat(productService.existsById(99L)).isFalse();
        }
    }

    // ── getProducts – multiple ids ────────────────────────────────────────────

    @Nested
    class GetProductsMultipleTest {

        @Test
        void getProducts_returnsAllProducts() {
            List<Long> ids = List.of(1L, 2L);
            List<ProductThumbnailVm> vms = List.of(
                    new ProductThumbnailVm(1L, "A", "a", "a.jpg"),
                    new ProductThumbnailVm(2L, "B", "b", "b.jpg")
            );
            mockGetProducts(ids, vms);

            List<ProductThumbnailVm> result = productService.getProducts(ids);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(1).id()).isEqualTo(2L);
        }
    }
}
