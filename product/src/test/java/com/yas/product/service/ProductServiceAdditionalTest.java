package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.model.ProductRelated;
import com.yas.product.model.attribute.ProductAttribute;
import com.yas.product.model.attribute.ProductAttributeGroup;
import com.yas.product.model.attribute.ProductAttributeValue;
import com.yas.product.model.enumeration.FilterExistInWhSelection;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailVm;
import com.yas.product.viewmodel.product.ProductEsDetailVm;
import com.yas.product.viewmodel.product.ProductFeatureGetVm;
import com.yas.product.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.product.viewmodel.product.ProductListGetFromCategoryVm;
import com.yas.product.viewmodel.product.ProductListVm;
import com.yas.product.viewmodel.product.ProductQuantityPutVm;
import com.yas.product.viewmodel.product.ProductsGetVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceAdditionalTest {

    @Mock private ProductRepository productRepository;
    @Mock private MediaService mediaService;
    @Mock private BrandRepository brandRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductCategoryRepository productCategoryRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductOptionRepository productOptionRepository;
    @Mock private ProductOptionValueRepository productOptionValueRepository;
    @Mock private ProductOptionCombinationRepository productOptionCombinationRepository;
    @Mock private ProductRelatedRepository productRelatedRepository;

    @InjectMocks
    private ProductService productService;

    // ── getProductById ────────────────────────────────────────────────────────

    @Test
    void getProductById_whenFound_returnsDetailVm() {
        Brand brand = new Brand();
        brand.setId(2L);

        Category category = new Category();
        category.setName("Cat");

        Product product = Product.builder()
                .id(1L).name("Phone").slug("phone").price(100.0)
                .thumbnailMediaId(5L).build();
        product.setBrand(brand);
        product.setProductCategories(
                List.of(ProductCategory.builder().product(product).category(category).build()));
        product.setProductImages(
                List.of(ProductImage.builder().imageId(10L).product(product).build()));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(5L)).thenReturn(new NoFileMediaVm(5L, "", "", "", "thumb"));
        when(mediaService.getMedia(10L)).thenReturn(new NoFileMediaVm(10L, "", "", "", "img"));

        ProductDetailVm result = productService.getProductById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.brandId()).isEqualTo(2L);
        assertThat(result.categories()).hasSize(1);
        assertThat(result.thumbnailMedia().url()).isEqualTo("thumb");
        assertThat(result.productImageMedias()).hasSize(1);
    }

    @Test
    void getProductById_whenNotFound_throwsNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void getProductById_whenNoBrandAndNoImages_returnsVmWithNulls() {
        Product product = Product.builder().id(1L).name("P").slug("p").build();
        product.setProductCategories(List.of());
        product.setProductImages(null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailVm result = productService.getProductById(1L);

        assertThat(result.brandId()).isNull();
        assertThat(result.thumbnailMedia()).isNull();
        assertThat(result.productImageMedias()).isEmpty();
    }

    // ── getLatestProducts ─────────────────────────────────────────────────────

    @Test
    void getLatestProducts_whenProductsExist_returnsList() {
        Product p = Product.builder().id(1L).name("P").slug("p").build();
        when(productRepository.getLatestProducts(PageRequest.of(0, 5))).thenReturn(List.of(p));

        var result = productService.getLatestProducts(5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("P");
    }

    // ── getProductsFromCategory ───────────────────────────────────────────────

    @Test
    void getProductsFromCategory_whenCategoryNotFound_throwsNotFoundException() {
        when(categoryRepository.findBySlug("missing")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> productService.getProductsFromCategory(0, 5, "missing"));
    }

    @Test
    void getProductsFromCategory_returnsPagedThumbnails() {
        Category cat = new Category();
        cat.setId(1L);
        cat.setSlug("electronics");

        Product product = Product.builder().id(10L).name("TV").slug("tv").thumbnailMediaId(3L).build();
        ProductCategory pc = ProductCategory.builder().product(product).category(cat).build();

        Page<ProductCategory> page = new PageImpl<>(List.of(pc), PageRequest.of(0, 5), 1);

        when(categoryRepository.findBySlug("electronics")).thenReturn(Optional.of(cat));
        when(productCategoryRepository.findAllByCategory(any(Pageable.class), any())).thenReturn(page);
        when(mediaService.getMedia(3L)).thenReturn(new NoFileMediaVm(3L, "", "", "", "tv-url"));

        ProductListGetFromCategoryVm result = productService.getProductsFromCategory(0, 5, "electronics");

        assertThat(result.productContent()).hasSize(1);
        assertThat(result.productContent().get(0).thumbnailUrl()).isEqualTo("tv-url");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    // ── getListFeaturedProducts ───────────────────────────────────────────────

    @Test
    void getListFeaturedProducts_returnsFeaturedPage() {
        Product p = Product.builder().id(1L).name("P").slug("p").thumbnailMediaId(7L).price(50.0).build();
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 3), 1);

        when(productRepository.getFeaturedProduct(any(Pageable.class))).thenReturn(page);
        when(mediaService.getMedia(7L)).thenReturn(new NoFileMediaVm(7L, "", "", "", "feat-url"));

        ProductFeatureGetVm result = productService.getListFeaturedProducts(0, 3);

        assertThat(result.productList()).hasSize(1);
        assertThat(result.productList().get(0).thumbnailUrl()).isEqualTo("feat-url");
        assertThat(result.totalPage()).isEqualTo(1);
    }

    // ── getProductDetail - not found ──────────────────────────────────────────

    @Test
    void getProductDetail_whenNotFound_throwsNotFoundException() {
        when(productRepository.findBySlugAndIsPublishedTrue("ghost")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> productService.getProductDetail("ghost"));
    }

    // ── deleteProduct - no parent ─────────────────────────────────────────────

    @Test
    void deleteProduct_whenNoParent_doesNotDeleteCombinations() {
        Product product = Product.builder().id(5L).isPublished(true).build();
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        productService.deleteProduct(5L);

        assertThat(product.isPublished()).isFalse();
        verify(productOptionCombinationRepository, never()).findAllByProduct(any());
        verify(productRepository).save(product);
    }

    @Test
    void deleteProduct_whenNotFound_throwsNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> productService.deleteProduct(99L));
    }

    // ── exportProducts ────────────────────────────────────────────────────────

    @Test
    void exportProducts_returnsMappedList() {
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("BrandX");

        Product p = Product.builder()
                .id(1L).name("P").slug("p").sku("sku1").gtin("g1")
                .price(10.0).isPublished(true).isFeatured(false)
                .isAllowedToOrder(true).isVisibleIndividually(true)
                .stockTrackingEnabled(false).metaTitle("mt")
                .metaKeyword("mk").metaDescription("md").build();
        p.setBrand(brand);

        when(productRepository.getExportingProducts("laptop", "BrandX"))
                .thenReturn(List.of(p));

        var result = productService.exportProducts(" Laptop ", " BrandX ");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("P");
        assertThat(result.get(0).brandName()).isEqualTo("BrandX");
    }

    // ── getProductEsDetailById ────────────────────────────────────────────────

    @Test
    void getProductEsDetailById_whenFound_returnsVm() {
        Brand brand = new Brand();
        brand.setName("Nike");

        Category cat = new Category();
        cat.setName("Shoes");
        Product product = Product.builder().id(1L).name("Air Max").slug("air-max").price(120.0)
                .isPublished(true).isVisibleIndividually(true).isAllowedToOrder(true)
                .isFeatured(false).thumbnailMediaId(9L).build();
        product.setBrand(brand);
        product.setProductCategories(
                List.of(ProductCategory.builder().product(product).category(cat).build()));

        ProductAttribute attr = ProductAttribute.builder().name("Color").build();
        ProductAttributeValue attrVal = new ProductAttributeValue();
        attrVal.setProductAttribute(attr);
        attrVal.setValue("Red");
        product.setAttributeValues(List.of(attrVal));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductEsDetailVm result = productService.getProductEsDetailById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.brand()).isEqualTo("Nike");
        assertThat(result.categories()).containsExactly("Shoes");
        assertThat(result.attributes()).containsExactly("Color");
        assertThat(result.thumbnailMediaId()).isEqualTo(9L);
    }

    @Test
    void getProductEsDetailById_whenNotFound_throwsNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> productService.getProductEsDetailById(99L));
    }

    @Test
    void getProductEsDetailById_whenNoBrand_returnsBrandNameNull() {
        Product product = Product.builder().id(2L).name("P").slug("p").build();
        product.setProductCategories(List.of());
        product.setAttributeValues(List.of());

        when(productRepository.findById(2L)).thenReturn(Optional.of(product));

        ProductEsDetailVm result = productService.getProductEsDetailById(2L);

        assertThat(result.brand()).isNull();
        assertThat(result.thumbnailMediaId()).isNull();
    }

    // ── getRelatedProductsBackoffice ──────────────────────────────────────────

    @Test
    void getRelatedProductsBackoffice_whenFound_returnsList() {
        Product related = Product.builder().id(2L).name("R").slug("r").price(20.0).build();
        ProductRelated pr = ProductRelated.builder().product(null).relatedProduct(related).build();
        Product product = Product.builder().id(1L).build();
        product.setRelatedProducts(List.of(pr));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        List<ProductListVm> result = productService.getRelatedProductsBackoffice(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(2L);
    }

    @Test
    void getRelatedProductsBackoffice_whenNotFound_throwsNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> productService.getRelatedProductsBackoffice(99L));
    }

    // ── getRelatedProductsStorefront ──────────────────────────────────────────

    @Test
    void getRelatedProductsStorefront_returnsOnlyPublished() {
        Product published = Product.builder().id(2L).name("R").slug("r")
                .thumbnailMediaId(5L).price(30.0).isPublished(true).build();
        Product unpublished = Product.builder().id(3L).isPublished(false).build();

        ProductRelated pr1 = ProductRelated.builder().relatedProduct(published).build();
        ProductRelated pr2 = ProductRelated.builder().relatedProduct(unpublished).build();

        Product product = Product.builder().id(1L).build();
        Page<ProductRelated> page = new PageImpl<>(List.of(pr1, pr2), PageRequest.of(0, 5), 2);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRelatedRepository.findAllByProduct(any(), any(Pageable.class))).thenReturn(page);
        when(mediaService.getMedia(5L)).thenReturn(new NoFileMediaVm(5L, "", "", "", "url"));

        ProductsGetVm result = productService.getRelatedProductsStorefront(1L, 0, 5);

        assertThat(result.productContent()).hasSize(1);
        assertThat(result.productContent().get(0).id()).isEqualTo(2L);
    }

    @Test
    void getRelatedProductsStorefront_whenNotFound_throwsNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> productService.getRelatedProductsStorefront(99L, 0, 5));
    }

    // ── getProductsForWarehouse ───────────────────────────────────────────────

    @Test
    void getProductsForWarehouse_returnsMappedList() {
        Product p = Product.builder().id(1L).name("W").slug("w").sku("s").price(10.0)
                .isPublished(true).isAllowedToOrder(true).stockTrackingEnabled(true)
                .stockQuantity(100L).build();

        when(productRepository.findProductForWarehouse("w", "s", List.of(1L), "ALL"))
                .thenReturn(List.of(p));

        var result = productService.getProductsForWarehouse("w", "s", List.of(1L),
                FilterExistInWhSelection.ALL);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("W");
    }

    // ── restoreStockQuantity ──────────────────────────────────────────────────

    @Test
    void restoreStockQuantity_addsQuantityToTrackedProducts() {
        Product p = Product.builder().id(1L).stockQuantity(10L).stockTrackingEnabled(true).build();
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(p));

        productService.restoreStockQuantity(List.of(new ProductQuantityPutVm(1L, 5L)));

        assertThat(p.getStockQuantity()).isEqualTo(15L);
        verify(productRepository).saveAll(List.of(p));
    }

    @Test
    void restoreStockQuantity_doesNotUpdateNonTrackedProducts() {
        Product p = Product.builder().id(1L).stockQuantity(10L).stockTrackingEnabled(false).build();
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(p));

        productService.restoreStockQuantity(List.of(new ProductQuantityPutVm(1L, 5L)));

        assertThat(p.getStockQuantity()).isEqualTo(10L); // unchanged
    }

    // ── subtractStockQuantity – clamps to zero ────────────────────────────────

    @Test
    void subtractStockQuantity_clampsToZeroWhenResultNegative() {
        Product p = Product.builder().id(1L).stockQuantity(3L).stockTrackingEnabled(true).build();
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(p));

        productService.subtractStockQuantity(List.of(new ProductQuantityPutVm(1L, 10L)));

        assertThat(p.getStockQuantity()).isEqualTo(0L);
    }

    // ── getProductByIds ───────────────────────────────────────────────────────

    @Test
    void getProductByIds_returnsMappedList() {
        Product p = Product.builder().id(1L).name("P").slug("p").build();
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(p));

        List<ProductListVm> result = productService.getProductByIds(List.of(1L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    // ── getProductByCategoryIds ───────────────────────────────────────────────

    @Test
    void getProductByCategoryIds_returnsMappedList() {
        Product p = Product.builder().id(1L).name("P").slug("p").build();
        when(productRepository.findByCategoryIdsIn(List.of(1L))).thenReturn(List.of(p));

        List<ProductListVm> result = productService.getProductByCategoryIds(List.of(1L));

        assertThat(result).hasSize(1);
    }

    // ── getProductByBrandIds ──────────────────────────────────────────────────

    @Test
    void getProductByBrandIds_returnsMappedList() {
        Product p = Product.builder().id(1L).name("P").slug("p").build();
        when(productRepository.findByBrandIdsIn(List.of(1L))).thenReturn(List.of(p));

        List<ProductListVm> result = productService.getProductByBrandIds(List.of(1L));

        assertThat(result).hasSize(1);
    }

    // ── getProductCheckoutList ────────────────────────────────────────────────

    @Test
    void getProductCheckoutList_withThumbnail_returnsVmWithUrl() {
        Brand brand = new Brand();
        brand.setId(1L);

        Product p = Product.builder().id(1L).name("P").slug("p").price(99.0)
                .thumbnailMediaId(4L).build();
        p.setBrand(brand);

        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 5), 1);
        when(productRepository.findAllPublishedProductsByIds(List.of(1L), PageRequest.of(0, 5)))
                .thenReturn(page);
        when(mediaService.getMedia(4L)).thenReturn(new NoFileMediaVm(4L, "", "", "", "thumb-url"));

        ProductGetCheckoutListVm result = productService.getProductCheckoutList(0, 5, List.of(1L));

        assertThat(result.productCheckoutListVms()).hasSize(1);
        assertThat(result.productCheckoutListVms().get(0).thumbnailUrl()).isEqualTo("thumb-url");
    }

    @Test
    void getProductCheckoutList_withoutThumbnail_returnsEmptyUrl() {
        Brand brand = new Brand();
        brand.setId(1L);

        Product p = Product.builder().id(1L).name("P").slug("p").price(99.0)
                .thumbnailMediaId(4L).build();
        p.setBrand(brand);

        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 5), 1);
        when(productRepository.findAllPublishedProductsByIds(List.of(1L), PageRequest.of(0, 5)))
                .thenReturn(page);
        when(mediaService.getMedia(4L)).thenReturn(new NoFileMediaVm(4L, "", "", "", ""));

        ProductGetCheckoutListVm result = productService.getProductCheckoutList(0, 5, List.of(1L));

        assertThat(result.productCheckoutListVms()).hasSize(1);
        assertThat(result.productCheckoutListVms().get(0).thumbnailUrl()).isEmpty();
    }

    // ── getProductSlug – not found ────────────────────────────────────────────

    @Test
    void getProductSlug_whenNotFound_throwsNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> productService.getProductSlug(99L));
    }

    // ── getProductVariationsByParentId – not found ────────────────────────────

    @Test
    void getProductVariationsByParentId_whenNotFound_throwsNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> productService.getProductVariationsByParentId(99L));
    }

    // ── setProductImages – null existing images ───────────────────────────────

    @Test
    void setProductImages_whenProductImagesNull_createsNewImages() {
        Product product = Product.builder().id(5L).build();
        product.setProductImages(null);

        List<ProductImage> result = productService.setProductImages(List.of(1L, 2L), product);

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(ProductImage::getImageId).toList())
                .containsExactlyInAnyOrder(1L, 2L);
    }

    // ── getProductDetail with no attribute values and no images ───────────────

    @Test
    void getProductDetail_withNoAttributesAndNoImages_returnsEmptyCollections() {
        Product product = Product.builder()
                .id(1L).name("P").slug("p")
                .isAllowedToOrder(true).isPublished(true).isFeatured(false)
                .hasOptions(false).price(10.0).thumbnailMediaId(2L).build();
        product.setAttributeValues(List.of());
        product.setProductImages(List.of());
        product.setProductCategories(List.of());

        when(productRepository.findBySlugAndIsPublishedTrue("p")).thenReturn(Optional.of(product));
        when(mediaService.getMedia(2L)).thenReturn(new NoFileMediaVm(2L, "", "", "", "t"));

        var result = productService.getProductDetail("p");

        assertThat(result.productAttributeGroups()).isEmpty();
        assertThat(result.productImageMediaUrls()).isEmpty();
    }

    // ── getFeaturedProductsById – thumbnail present (no parent fallback) ──────

    @Test
    void getFeaturedProductsById_whenThumbnailPresent_returnsSelfUrl() {
        Product p = Product.builder().id(1L).name("P").slug("p").thumbnailMediaId(3L)
                .price(10.0).build();

        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(p));
        when(mediaService.getMedia(3L)).thenReturn(new NoFileMediaVm(3L, "", "", "", "self-url"));

        var result = productService.getFeaturedProductsById(List.of(1L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).thumbnailUrl()).isEqualTo("self-url");
    }

    // ── getProductsByBrand - empty product list ───────────────────────────────

    @Test
    void getProductsByBrand_whenNoProducts_returnsEmptyList() {
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setSlug("brand");
        when(brandRepository.findBySlug("brand")).thenReturn(Optional.of(brand));
        when(productRepository.findAllByBrandAndIsPublishedTrueOrderByIdAsc(brand))
                .thenReturn(List.of());

        var result = productService.getProductsByBrand("brand");

        assertThat(result).isEmpty();
    }

    // ── subtractStockQuantity with duplicate product ids (merge) ─────────────

    @Test
    void subtractStockQuantity_mergesDuplicateProductIds() {
        Product p = Product.builder().id(1L).stockQuantity(20L).stockTrackingEnabled(true).build();
        when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(p));

        // Same product appears twice – quantities should be merged (3+2=5)
        productService.subtractStockQuantity(List.of(
                new ProductQuantityPutVm(1L, 3L),
                new ProductQuantityPutVm(1L, 2L)
        ));

        assertThat(p.getStockQuantity()).isEqualTo(15L);
    }
}
