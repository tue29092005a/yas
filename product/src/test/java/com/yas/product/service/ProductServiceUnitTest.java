package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.model.attribute.ProductAttribute;
import com.yas.product.model.attribute.ProductAttributeGroup;
import com.yas.product.model.attribute.ProductAttributeValue;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.utils.Constants;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailGetVm;
import com.yas.product.viewmodel.product.ProductListGetVm;
import com.yas.product.viewmodel.product.ProductPostVm;
import com.yas.product.viewmodel.product.ProductQuantityPostVm;
import com.yas.product.viewmodel.product.ProductQuantityPutVm;
import com.yas.product.viewmodel.product.ProductSlugGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailGetVm;
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

@ExtendWith(MockitoExtension.class)
class ProductServiceUnitTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private MediaService mediaService;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductCategoryRepository productCategoryRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private ProductOptionRepository productOptionRepository;
    @Mock
    private ProductOptionValueRepository productOptionValueRepository;
    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;
    @Mock
    private ProductRelatedRepository productRelatedRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_whenLengthLessThanWidth_throwsBadRequest() {
        ProductPostVm postVm = new ProductPostVm(
                "Name",
                "slug",
                null,
                List.of(),
                "short",
                "description",
                "spec",
                "sku",
                "gtin",
                1.0,
                null,
                1.0,
                2.0,
                3.0,
                10.0,
                true,
                true,
                false,
                true,
                true,
                "meta",
                "keyword",
                "metaDesc",
                1L,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                1L
        );

        assertThrows(BadRequestException.class, () -> productService.createProduct(postVm));
        verifyNoInteractions(productRepository, brandRepository, categoryRepository);
    }

    @Test
    void getLatestProducts_whenCountNonPositive_returnsEmpty() {
        assertThat(productService.getLatestProducts(0)).isEmpty();
        verifyNoInteractions(productRepository);
    }

    @Test
    void getLatestProducts_whenRepositoryReturnsEmpty_returnsEmpty() {
        when(productRepository.getLatestProducts(PageRequest.of(0, 3))).thenReturn(List.of());

        assertThat(productService.getLatestProducts(3)).isEmpty();
    }

    @Test
    void getProductsByBrand_returnsThumbnails() {
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setSlug("brand");
        when(brandRepository.findBySlug("brand")).thenReturn(Optional.of(brand));

        Product product = Product.builder().id(10L).name("Phone").slug("phone").thumbnailMediaId(5L).build();
        when(productRepository.findAllByBrandAndIsPublishedTrueOrderByIdAsc(brand)).thenReturn(List.of(product));
        when(mediaService.getMedia(5L)).thenReturn(new NoFileMediaVm(5L, "", "", "", "url-5"));

        var result = productService.getProductsByBrand("brand");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).thumbnailUrl()).isEqualTo("url-5");
    }

    @Test
    void getProductsByBrand_whenNotFound_throwsNotFound() {
        when(brandRepository.findBySlug("brand")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductsByBrand("brand"));
    }

    @Test
    void getProductSlug_whenHasParent_returnsParentSlug() {
        Product parent = Product.builder().id(1L).slug("parent").build();
        Product child = Product.builder().id(2L).slug("child").parent(parent).build();
        when(productRepository.findById(2L)).thenReturn(Optional.of(child));

        ProductSlugGetVm result = productService.getProductSlug(2L);

        assertThat(result.slug()).isEqualTo("parent");
        assertThat(result.productVariantId()).isEqualTo(2L);
    }

    @Test
    void getProductSlug_whenNoParent_returnsSelfSlug() {
        Product product = Product.builder().id(1L).slug("self").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductSlugGetVm result = productService.getProductSlug(1L);

        assertThat(result.slug()).isEqualTo("self");
        assertThat(result.productVariantId()).isNull();
    }

    @Test
    void getProductVariationsByParentId_returnsPublishedVariations() {
        Product parent = Product.builder().id(1L).hasOptions(true).build();

        Product variation = Product.builder()
                .id(2L)
                .name("Var")
                .slug("var")
                .sku("sku")
                .gtin("gtin")
                .price(10.0)
                .isPublished(true)
                .thumbnailMediaId(11L)
                .build();
        variation.setProductImages(List.of(ProductImage.builder().imageId(21L).product(variation).build()));

        Product hidden = Product.builder().id(3L).isPublished(false).build();
        parent.setProducts(List.of(variation, hidden));

        ProductOption option = new ProductOption();
        option.setId(100L);
        ProductOptionCombination combination = ProductOptionCombination.builder()
                .product(variation)
                .productOption(option)
                .value("M")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(productOptionCombinationRepository.findAllByProduct(variation))
                .thenReturn(List.of(combination));
        when(mediaService.getMedia(anyLong()))
                .thenAnswer(invocation -> new NoFileMediaVm(invocation.getArgument(0), "", "", "", "url"));

        var result = productService.getProductVariationsByParentId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).options()).containsEntry(100L, "M");
        assertThat(result.get(0).productImages()).hasSize(1);
    }

    @Test
    void getProductVariationsByParentId_whenNoOptions_returnsEmpty() {
        Product parent = Product.builder().id(1L).hasOptions(false).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(parent));

        assertThat(productService.getProductVariationsByParentId(1L)).isEmpty();
    }

    @Test
    void getProductsWithFilter_returnsPagedContent() {
        Product product = Product.builder().id(1L).name("Phone").slug("phone").build();
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 2), 1);
        when(productRepository.getProductsWithFilter(eq("phone"), eq("Brand"), eq(PageRequest.of(0, 2))))
                .thenReturn(page);

        ProductListGetVm result = productService.getProductsWithFilter(0, 2, " Phone ", "Brand ");

        assertThat(result.productContent()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getProductsByMultiQuery_returnsPagedContent() {
        Product product = Product.builder().id(1L).name("Phone").slug("phone").thumbnailMediaId(9L).price(10.0)
                .build();
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 2), 1);
        when(productRepository.findByProductNameAndCategorySlugAndPriceBetween(
                eq("phone"), eq("cat"), eq(1.0), eq(20.0), eq(PageRequest.of(0, 2))
        )).thenReturn(page);
        when(mediaService.getMedia(9L)).thenReturn(new NoFileMediaVm(9L, "", "", "", "url-9"));

        ProductsGetVm result = productService.getProductsByMultiQuery(0, 2, " Phone ", "cat", 1.0, 20.0);

        assertThat(result.productContent()).hasSize(1);
        assertThat(result.productContent().get(0).thumbnailUrl()).isEqualTo("url-9");
    }

    @Test
    void getFeaturedProductsById_usesParentThumbnailWhenMissing() {
        Product parent = Product.builder().id(1L).slug("parent").thumbnailMediaId(5L).build();
        Product child = Product.builder().id(2L).slug("child").thumbnailMediaId(6L).parent(parent).price(9.0)
                .build();

        when(productRepository.findAllByIdIn(List.of(2L))).thenReturn(List.of(child));
        when(productRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(mediaService.getMedia(6L)).thenReturn(new NoFileMediaVm(6L, "", "", "", ""));
        when(mediaService.getMedia(5L)).thenReturn(new NoFileMediaVm(5L, "", "", "", "parent-url"));

        List<ProductThumbnailGetVm> result = productService.getFeaturedProductsById(List.of(2L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).thumbnailUrl()).isEqualTo("parent-url");
    }

    @Test
    void getProductDetail_groupsAttributesAndImages() {
        Product product = Product.builder()
                .id(1L)
                .name("Phone")
                .shortDescription("short")
                .description("desc")
                .specification("spec")
                .isAllowedToOrder(true)
                .isPublished(true)
                .isFeatured(false)
                .hasOptions(false)
                .price(10.0)
                .thumbnailMediaId(8L)
                .build();

        Brand brand = new Brand();
        brand.setName("Brand");
        product.setBrand(brand);

        Category category = new Category();
        category.setName("Cat");
        product.setProductCategories(List.of(ProductCategory.builder().product(product).category(category).build()));

        ProductAttributeGroup group = new ProductAttributeGroup();
        group.setName("Specs");
        ProductAttribute attrWithGroup = ProductAttribute.builder().name("Color").productAttributeGroup(group).build();
        ProductAttribute attrNoGroup = ProductAttribute.builder().name("Weight").productAttributeGroup(null).build();

        ProductAttributeValue value1 = new ProductAttributeValue();
        value1.setProduct(product);
        value1.setProductAttribute(attrWithGroup);
        value1.setValue("Red");

        ProductAttributeValue value2 = new ProductAttributeValue();
        value2.setProduct(product);
        value2.setProductAttribute(attrNoGroup);
        value2.setValue("1kg");

        product.setAttributeValues(List.of(value1, value2));
        product.setProductImages(List.of(ProductImage.builder().imageId(11L).product(product).build()));

        when(productRepository.findBySlugAndIsPublishedTrue("phone")).thenReturn(Optional.of(product));
        when(mediaService.getMedia(8L)).thenReturn(new NoFileMediaVm(8L, "", "", "", "thumb"));
        when(mediaService.getMedia(11L)).thenReturn(new NoFileMediaVm(11L, "", "", "", "img"));

        ProductDetailGetVm result = productService.getProductDetail("phone");

        assertThat(result.productAttributeGroups()).hasSize(2);
        assertThat(result.productImageMediaUrls()).containsExactly("img");
        assertThat(result.thumbnailMediaUrl()).isEqualTo("thumb");
    }

    @Test
    void deleteProduct_whenParent_removesOptionCombinations() {
        Product parent = Product.builder().id(1L).build();
        Product child = Product.builder().id(2L).parent(parent).isPublished(true).build();
        ProductOptionCombination combination = ProductOptionCombination.builder().product(child).value("M").build();

        when(productRepository.findById(2L)).thenReturn(Optional.of(child));
        when(productOptionCombinationRepository.findAllByProduct(child)).thenReturn(List.of(combination));

        productService.deleteProduct(2L);

        assertThat(child.isPublished()).isFalse();
        verify(productOptionCombinationRepository).deleteAll(List.of(combination));
        verify(productRepository).save(child);
    }

    @Test
    void setProductImages_whenEmpty_deletesExisting() {
        Product product = Product.builder().id(3L).build();

        List<ProductImage> result = productService.setProductImages(List.of(), product);

        assertThat(result).isEmpty();
        verify(productImageRepository).deleteByProductId(3L);
    }

    @Test
    void setProductImages_whenExisting_updatesAndDeletes() {
        Product product = Product.builder().id(4L).build();
        product.setProductImages(List.of(
                ProductImage.builder().imageId(1L).product(product).build(),
                ProductImage.builder().imageId(2L).product(product).build(),
                ProductImage.builder().imageId(3L).product(product).build()
        ));

        List<ProductImage> result = productService.setProductImages(List.of(2L, 4L), product);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getImageId()).isEqualTo(4L);
        verify(productImageRepository).deleteByImageIdInAndProductId(List.of(1L, 3L), 4L);
    }

    @Test
    void updateProductQuantity_updatesStockQuantities() {
        Product product1 = Product.builder().id(1L).stockQuantity(5L).build();
        Product product2 = Product.builder().id(2L).stockQuantity(7L).build();
        when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(product1, product2));

        productService.updateProductQuantity(List.of(
                new ProductQuantityPostVm(1L, 10L),
                new ProductQuantityPostVm(2L, 20L)
        ));

        assertThat(product1.getStockQuantity()).isEqualTo(10L);
        assertThat(product2.getStockQuantity()).isEqualTo(20L);
        verify(productRepository).saveAll(List.of(product1, product2));
    }

    @Test
    void subtractStockQuantity_updatesTrackedProducts() {
        Product product = Product.builder().id(1L).stockQuantity(10L).stockTrackingEnabled(true).build();
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));

        productService.subtractStockQuantity(List.of(new ProductQuantityPutVm(1L, 3L)));

        assertThat(product.getStockQuantity()).isEqualTo(7L);
        verify(productRepository).saveAll(List.of(product));
    }
}
