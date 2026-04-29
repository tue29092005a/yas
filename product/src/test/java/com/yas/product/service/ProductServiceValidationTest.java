package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductOption;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.product.ProductGetDetailVm;
import com.yas.product.viewmodel.product.ProductOptionValueDisplay;
import com.yas.product.viewmodel.product.ProductPostVm;
import com.yas.product.viewmodel.product.ProductPutVm;
import com.yas.product.viewmodel.product.ProductVariationPostVm;
import com.yas.product.viewmodel.productoption.ProductOptionValuePostVm;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceValidationTest {

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

    // ── helpers ───────────────────────────────────────────────────────────────

    private ProductPostVm validPostVm() {
        return new ProductPostVm(
                "Phone", "phone-slug", 1L, new ArrayList<>(List.of(10L)),
                "short", "desc", "spec", "sku-1", "gtin-1",
                1.0, null, 3.0, 2.0, 1.0, 100.0,
                true, true, false, true, true,
                "meta", "keyword", "metaDesc",
                5L, List.of(), List.of(), List.of(), List.of(), List.of(), 1L);
    }

    private void mockAllConflictsEmpty() {
        lenient().when(productRepository.findBySlugAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        lenient().when(productRepository.findBySkuAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        lenient().when(productRepository.findByGtinAndIsPublishedTrue(any())).thenReturn(Optional.empty());
    }

    private Product savedProduct() {
        Product s = Product.builder().id(1L).name("Phone").slug("phone-slug").build();
        s.setProductCategories(List.of());
        return s;
    }

    // ── createProduct – happy path (no variations) ────────────────────────────

    @Test
    void createProduct_noVariations_savesAndReturns() {
        mockAllConflictsEmpty();
        when(productRepository.findAllById(List.of())).thenReturn(List.of());

        Brand brand = new Brand(); brand.setId(1L);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        Category cat = new Category(); cat.setId(10L);
        when(categoryRepository.findAllById(any())).thenReturn(List.of(cat));

        when(productRepository.save(any())).thenReturn(savedProduct());
        when(productImageRepository.saveAll(anyList())).thenReturn(List.of());
        when(productCategoryRepository.saveAll(anyList())).thenReturn(List.of());

        ProductGetDetailVm result = productService.createProduct(validPostVm());

        assertThat(result.id()).isEqualTo(1L);
        verify(productRepository).save(any(Product.class));
    }

    // ── createProduct – brand not found ───────────────────────────────────────

    @Test
    void createProduct_whenBrandNotFound_throwsNotFoundException() {
        mockAllConflictsEmpty();
        when(productRepository.findAllById(List.of())).thenReturn(List.of());
        when(brandRepository.findById(1L)).thenReturn(Optional.empty());
        when(productRepository.save(any())).thenReturn(savedProduct());
        when(productImageRepository.saveAll(anyList())).thenReturn(List.of());
        when(productCategoryRepository.saveAll(anyList())).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> productService.createProduct(validPostVm()));
    }

    // ── createProduct – duplicate slug in DB ─────────────────────────────────

    @Test
    void createProduct_whenSlugExistsInDb_throwsDuplicatedException() {
        Product existing = Product.builder().id(999L).slug("phone-slug").build();
        lenient().when(productRepository.findBySlugAndIsPublishedTrue("phone-slug"))
                .thenReturn(Optional.of(existing));
        lenient().when(productRepository.findBySkuAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        lenient().when(productRepository.findByGtinAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findAllById(List.of())).thenReturn(List.of());

        assertThrows(DuplicatedException.class, () -> productService.createProduct(validPostVm()));
    }

    // ── createProduct – duplicate SKU in DB ──────────────────────────────────

    @Test
    void createProduct_whenSkuExistsInDb_throwsDuplicatedException() {
        Product existing = Product.builder().id(999L).build();
        lenient().when(productRepository.findBySlugAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        lenient().when(productRepository.findByGtinAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findBySkuAndIsPublishedTrue("sku-1")).thenReturn(Optional.of(existing));
        when(productRepository.findAllById(List.of())).thenReturn(List.of());

        assertThrows(DuplicatedException.class, () -> productService.createProduct(validPostVm()));
    }

    // ── createProduct – duplicate GTIN in DB ─────────────────────────────────

    @Test
    void createProduct_whenGtinExistsInDb_throwsDuplicatedException() {
        Product existing = Product.builder().id(999L).build();
        lenient().when(productRepository.findBySlugAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        lenient().when(productRepository.findBySkuAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findByGtinAndIsPublishedTrue("gtin-1")).thenReturn(Optional.of(existing));
        when(productRepository.findAllById(List.of())).thenReturn(List.of());

        assertThrows(DuplicatedException.class, () -> productService.createProduct(validPostVm()));
    }

    // ── validateProductVariationDuplicates – duplicate slug ──────────────────

    @Test
    void createProduct_whenVariationsDuplicateSlug_throwsDuplicatedException() {
        ProductVariationPostVm v1 = new ProductVariationPostVm(
                "V1", "dup-slug", "sku-v1", "", 10.0, null, List.of(), Map.of());
        ProductVariationPostVm v2 = new ProductVariationPostVm(
                "V2", "dup-slug", "sku-v2", "", 20.0, null, List.of(), Map.of());

        ProductPostVm vm = new ProductPostVm(
                "Phone", "phone-slug", null, List.of(),
                "short", "desc", "spec", "sku-main", "",
                1.0, null, 3.0, 2.0, 1.0, 100.0, true, true, false, true, true,
                "meta", "keyword", "metaDesc",
                null, List.of(), List.of(v1, v2), List.of(), List.of(), List.of(), 1L);

        lenient().when(productRepository.findAllById(anyList())).thenReturn(List.of());

        assertThrows(DuplicatedException.class, () -> productService.createProduct(vm));
    }

    // ── validateProductVariationDuplicates – duplicate SKU ───────────────────

    @Test
    void createProduct_whenVariationsDuplicateSku_throwsDuplicatedException() {
        ProductVariationPostVm v1 = new ProductVariationPostVm(
                "V1", "slug-v1", "dup-sku", "", 10.0, null, List.of(), Map.of());
        ProductVariationPostVm v2 = new ProductVariationPostVm(
                "V2", "slug-v2", "dup-sku", "", 20.0, null, List.of(), Map.of());

        ProductPostVm vm = new ProductPostVm(
                "Phone", "phone-slug", null, List.of(),
                "short", "desc", "spec", "sku-main", "",
                1.0, null, 3.0, 2.0, 1.0, 100.0, true, true, false, true, true,
                "meta", "keyword", "metaDesc",
                null, List.of(), List.of(v1, v2), List.of(), List.of(), List.of(), 1L);

        lenient().when(productRepository.findAllById(anyList())).thenReturn(List.of());

        assertThrows(DuplicatedException.class, () -> productService.createProduct(vm));
    }

    // ── validateProductVariationDuplicates – duplicate GTIN ──────────────────

    @Test
    void createProduct_whenVariationsDuplicateGtin_throwsDuplicatedException() {
        ProductVariationPostVm v1 = new ProductVariationPostVm(
                "V1", "slug-v1", "sku-v1", "dup-gtin", 10.0, null, List.of(), Map.of());
        ProductVariationPostVm v2 = new ProductVariationPostVm(
                "V2", "slug-v2", "sku-v2", "dup-gtin", 20.0, null, List.of(), Map.of());

        ProductPostVm vm = new ProductPostVm(
                "Phone", "phone-slug", null, List.of(),
                "short", "desc", "spec", "sku-main", "",
                1.0, null, 3.0, 2.0, 1.0, 100.0, true, true, false, true, true,
                "meta", "keyword", "metaDesc",
                null, List.of(), List.of(v1, v2), List.of(), List.of(), List.of(), 1L);

        lenient().when(productRepository.findAllById(anyList())).thenReturn(List.of());

        assertThrows(DuplicatedException.class, () -> productService.createProduct(vm));
    }

    // ── setProductCategories – all not found → BadRequestException ────────────

    @Test
    void createProduct_whenCategoriesNotFound_throwsBadRequestException() {
        mockAllConflictsEmpty();
        when(productRepository.findAllById(List.of())).thenReturn(List.of());
        when(brandRepository.findById(1L)).thenReturn(Optional.of(new Brand()));
        when(categoryRepository.findAllById(any())).thenReturn(List.of()); // not found
        when(productRepository.save(any())).thenReturn(savedProduct());
        when(productImageRepository.saveAll(anyList())).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> productService.createProduct(validPostVm()));
    }

    // ── setProductCategories – partial not found → BadRequestException ─────────

    @Test
    void createProduct_whenPartialCategoriesNotFound_throwsBadRequestException() {
        // Use mutable list so removeAll() doesn't throw UnsupportedOperationException
        ProductPostVm vm = new ProductPostVm(
                "Phone", "phone-slug", 1L, new ArrayList<>(List.of(10L, 20L)),
                "short", "desc", "spec", "sku-1", "gtin-1",
                1.0, null, 3.0, 2.0, 1.0, 100.0, true, true, false, true, true,
                "meta", "keyword", "metaDesc",
                null, List.of(), List.of(), List.of(), List.of(), List.of(), 1L);

        mockAllConflictsEmpty();
        when(productRepository.findAllById(List.of())).thenReturn(List.of());
        when(brandRepository.findById(1L)).thenReturn(Optional.of(new Brand()));

        // Only 1 of 2 categories found
        Category cat = new Category(); cat.setId(10L);
        when(categoryRepository.findAllById(any())).thenReturn(List.of(cat));
        when(productRepository.save(any())).thenReturn(savedProduct());
        when(productImageRepository.saveAll(anyList())).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> productService.createProduct(vm));
    }

    // ── createProduct – no brand (brandId null) ───────────────────────────────

    @Test
    void createProduct_whenNoBrand_doesNotCallBrandRepository() {
        ProductPostVm vm = new ProductPostVm(
                "Phone", "phone-slug", null, List.of(),
                "short", "desc", "spec", "sku-1", "gtin-1",
                1.0, null, 3.0, 2.0, 1.0, 100.0, true, true, false, true, true,
                "meta", "keyword", "metaDesc",
                null, List.of(), List.of(), List.of(), List.of(), List.of(), 1L);

        mockAllConflictsEmpty();
        when(productRepository.findAllById(List.of())).thenReturn(List.of());
        when(productRepository.save(any())).thenReturn(savedProduct());
        when(productImageRepository.saveAll(anyList())).thenReturn(List.of());
        when(productCategoryRepository.saveAll(anyList())).thenReturn(List.of());

        ProductGetDetailVm result = productService.createProduct(vm);

        assertThat(result).isNotNull();
        verifyNoInteractions(brandRepository);
    }

    // ── getProductOptionByIdMap – no options → BadRequestException ────────────

    @Test
    void createProduct_whenNoMatchingProductOptions_throwsBadRequestException() {
        ProductVariationPostVm variation = new ProductVariationPostVm(
                "V1", "slug-v1", "sku-v1", "", 10.0, null, List.of(), Map.of(1L, "Red"));
        ProductOptionValuePostVm optionVm = new ProductOptionValuePostVm(1L, "color", 1, List.of("Red"));
        ProductOptionValueDisplay displayVm = ProductOptionValueDisplay.builder()
                .productOptionId(1L).displayType("color").displayOrder(1).value("Red").build();

        ProductPostVm vm = new ProductPostVm(
                "Phone", "phone-slug", null, List.of(),
                "short", "desc", "spec", "sku-main", "",
                1.0, null, 3.0, 2.0, 1.0, 100.0, true, true, false, true, true,
                "meta", "keyword", "metaDesc",
                null, List.of(), List.of(variation),
                List.of(optionVm), List.of(displayVm), List.of(), 1L);

        lenient().when(productRepository.findBySlugAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        lenient().when(productRepository.findBySkuAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findAllById(anyList())).thenReturn(List.of());
        when(productRepository.save(any())).thenReturn(savedProduct());
        when(productRepository.saveAll(anyList())).thenReturn(List.of());
        when(productImageRepository.saveAll(anyList())).thenReturn(List.of());
        when(productCategoryRepository.saveAll(anyList())).thenReturn(List.of());
        when(productOptionRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> productService.createProduct(vm));
    }

    // ── updateMainProductFromVm – all fields updated ──────────────────────────

    @Test
    void updateMainProductFromVm_setsAllFields() {
        Product product = Product.builder().id(1L).build();
        ProductPutVm vm = new ProductPutVm(
                "Updated", "updated-slug", 200.0, true, true, false, true, true,
                null, List.of(), "short", "desc", "spec", "sku-u", "gtin-u",
                2.0, null, 4.0, 3.0, 1.0, "metaT", "metaK", "metaD",
                null, List.of(), List.of(), List.of(), List.of(), List.of(), 2L);

        productService.updateMainProductFromVm(vm, product);

        assertThat(product.getName()).isEqualTo("Updated");
        assertThat(product.getSlug()).isEqualTo("updated-slug");
        assertThat(product.getPrice()).isEqualTo(200.0);
        assertThat(product.getSku()).isEqualTo("sku-u");
        assertThat(product.getGtin()).isEqualTo("gtin-u");
        assertThat(product.getMetaTitle()).isEqualTo("metaT");
        assertThat(product.getTaxClassId()).isEqualTo(2L);
    }

    // ── updateProduct – not found ─────────────────────────────────────────────

    @Test
    void updateProduct_whenNotFound_throwsNotFoundException() {
        ProductPutVm vm = new ProductPutVm(
                "N", "s", 10.0, true, true, false, true, true,
                null, List.of(), "s", "d", "spec", "sku", "",
                1.0, null, 3.0, 2.0, 1.0, "mt", "mk", "md",
                null, List.of(), List.of(), List.of(), List.of(), List.of(), 1L);

        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> productService.updateProduct(99L, vm));
    }

    // ── updateProduct – happy path (with option values) ───────────────────────

    @Test
    void updateProduct_withOptionValues_callsSave() {
        Product existing = Product.builder().id(1L).slug("old").sku("old-sku").build();
        existing.setProductCategories(List.of());
        existing.setProductImages(List.of());
        existing.setProducts(List.of());
        existing.setRelatedProducts(List.of());

        com.yas.product.viewmodel.productoption.ProductOptionValuePutVm povVm =
                new com.yas.product.viewmodel.productoption.ProductOptionValuePutVm(
                        7L, "color", 1, List.of("Blue"));

        ProductPutVm vm = new ProductPutVm(
                "Updated", "new-slug", 50.0, true, true, false, true, true,
                null, List.of(), "short", "desc", "spec", "new-sku", "",
                1.0, null, 3.0, 2.0, 1.0, "mt", "mk", "md",
                null, List.of(), List.of(), List.of(povVm), List.of(), List.of(), 1L);

        ProductOption opt = new ProductOption(); opt.setId(7L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        lenient().when(productRepository.findBySlugAndIsPublishedTrue("new-slug"))
                .thenReturn(Optional.of(existing));
        lenient().when(productRepository.findBySkuAndIsPublishedTrue("new-sku"))
                .thenReturn(Optional.of(existing));
        when(productRepository.findAllById(List.of())).thenReturn(List.of());
        when(productCategoryRepository.findAllByProductId(1L)).thenReturn(List.of());
        when(productImageRepository.saveAll(anyList())).thenReturn(List.of());
        when(productCategoryRepository.saveAll(anyList())).thenReturn(List.of());
        doNothing().when(productOptionValueRepository).deleteAllByProductId(1L);
        when(productOptionValueRepository.saveAll(anyList())).thenReturn(List.of());
        when(productRepository.saveAll(anyList())).thenReturn(List.of());
        when(productOptionRepository.findAllByIdIn(List.of(7L))).thenReturn(List.of(opt));

        productService.updateProduct(1L, vm);

        verify(productRepository).findById(1L);
    }

    // ── createProduct – createProductRelations branch ─────────────────────────

    @Test
    void createProduct_withRelatedProducts_savesRelations() {
        ProductPostVm vm = new ProductPostVm(
                "Phone", "phone-slug", null, List.of(),
                "short", "desc", "spec", "sku-1", "",
                1.0, null, 3.0, 2.0, 1.0, 100.0, true, true, false, true, true,
                "meta", "keyword", "metaDesc",
                null, List.of(), List.of(), List.of(), List.of(),
                List.of(99L), // relatedProductIds
                1L);

        mockAllConflictsEmpty();
        when(productRepository.findAllById(List.of())).thenReturn(List.of()); // variation ids

        Product saved = savedProduct();
        when(productRepository.save(any())).thenReturn(saved);
        when(productImageRepository.saveAll(anyList())).thenReturn(List.of());
        when(productCategoryRepository.saveAll(anyList())).thenReturn(List.of());

        Product related = Product.builder().id(99L).build();
        when(productRepository.findAllById(List.of(99L))).thenReturn(List.of(related));
        when(productRelatedRepository.saveAll(anyList())).thenReturn(List.of());

        ProductGetDetailVm result = productService.createProduct(vm);

        assertThat(result).isNotNull();
        verify(productRelatedRepository).saveAll(anyList());
    }
}
