package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailInfoVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductDetailServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;

    @InjectMocks
    private ProductDetailService productDetailService;

    @Test
    void getProductDetailById_whenNotFound_throwsNotFoundException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productDetailService.getProductDetailById(1L));
    }

    @Test
    void getProductDetailById_whenNotPublished_throwsNotFoundException() {
        Product product = Product.builder().id(1L).isPublished(false).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(NotFoundException.class, () -> productDetailService.getProductDetailById(1L));
    }

    @Test
    void getProductDetailById_withoutOptions_returnsDetails() {
        Product product = Product.builder()
                .id(1L)
                .name("Main")
                .slug("main")
                .sku("sku-main")
                .gtin("gtin-main")
                .isAllowedToOrder(true)
                .isPublished(true)
                .isFeatured(true)
                .isVisibleIndividually(true)
                .stockTrackingEnabled(true)
                .price(100.0)
                .hasOptions(false)
                .thumbnailMediaId(10L)
                .taxClassId(2L)
                .build();

        Brand brand = new Brand();
        brand.setId(3L);
        brand.setName("Brand");
        product.setBrand(brand);

        Category category = new Category();
        category.setId(5L);
        category.setName("Cat");
        ProductCategory productCategory = ProductCategory.builder()
                .product(product)
                .category(category)
                .build();
        product.setProductCategories(List.of(productCategory));

        ProductImage image = ProductImage.builder()
                .imageId(20L)
                .product(product)
                .build();
        product.setProductImages(List.of(image));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(anyLong()))
                .thenAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return new NoFileMediaVm(id, "", "", "", "url-" + id);
                });

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getBrandId()).isEqualTo(3L);
        assertThat(result.getCategories()).hasSize(1);
        assertThat(result.getThumbnail().url()).isEqualTo("url-10");
        assertThat(result.getProductImages()).hasSize(1);
        assertThat(result.getProductImages().get(0).url()).isEqualTo("url-20");
        assertThat(result.getVariations()).isEmpty();
    }

    @Test
    void getProductDetailById_withOptions_buildsVariations() {
        Product product = Product.builder()
                .id(1L)
                .name("Main")
                .slug("main")
                .sku("sku-main")
                .gtin("gtin-main")
                .isAllowedToOrder(true)
                .isPublished(true)
                .isFeatured(true)
                .isVisibleIndividually(true)
                .stockTrackingEnabled(true)
                .price(100.0)
                .hasOptions(true)
                .thumbnailMediaId(10L)
                .build();

        Product variation = Product.builder()
                .id(2L)
                .name("Variant")
                .slug("variant")
                .sku("sku-variant")
                .gtin("gtin-variant")
                .price(80.0)
                .isPublished(true)
                .thumbnailMediaId(11L)
                .build();

        ProductImage variationImage = ProductImage.builder()
                .imageId(21L)
                .product(variation)
                .build();
        variation.setProductImages(List.of(variationImage));

        Product hiddenVariation = Product.builder()
                .id(3L)
                .name("Hidden")
                .isPublished(false)
                .build();

        product.setProducts(List.of(variation, hiddenVariation));

        ProductOption option = new ProductOption();
        option.setId(100L);
        option.setName("Size");
        ProductOptionCombination combination = ProductOptionCombination.builder()
                .product(variation)
                .productOption(option)
                .value("M")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productOptionCombinationRepository.findAllByProduct(variation))
                .thenReturn(List.of(combination));
        when(mediaService.getMedia(anyLong()))
                .thenAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    return new NoFileMediaVm(id, "", "", "", "url-" + id);
                });

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getVariations()).hasSize(1);
        assertThat(result.getVariations().get(0).options()).containsEntry(100L, "M");
        assertThat(result.getVariations().get(0).thumbnail().url()).isEqualTo("url-11");
        assertThat(result.getVariations().get(0).productImages()).hasSize(1);
    }
}
