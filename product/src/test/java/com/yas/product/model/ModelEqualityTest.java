package com.yas.product.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yas.product.model.attribute.ProductAttribute;
import com.yas.product.model.attribute.ProductAttributeGroup;
import com.yas.product.model.attribute.ProductTemplate;
import org.junit.jupiter.api.Test;

class ModelEqualityTest {

    @Test
    void product_equality_uses_id() {
        Product product1 = Product.builder().id(1L).build();
        Product product2 = Product.builder().id(1L).build();
        Product product3 = Product.builder().id(2L).build();
        Product productNoId = Product.builder().build();

        assertEquals(product1, product2);
        assertNotEquals(product1, product3);
        assertNotEquals(product1, new Object());
        assertNotEquals(product1, productNoId);
        assertNull(productNoId.getId());
        assertEquals(Product.class.hashCode(), product1.hashCode());
    }

    @Test
    void category_equality_uses_id() {
        Category category1 = new Category();
        category1.setId(1L);
        Category category2 = new Category();
        category2.setId(1L);
        Category category3 = new Category();
        category3.setId(2L);

        assertEquals(category1, category2);
        assertNotEquals(category1, category3);
        assertEquals(Category.class.hashCode(), category1.hashCode());
    }

    @Test
    void brand_equality_uses_id() {
        Brand brand1 = new Brand();
        brand1.setId(1L);
        Brand brand2 = new Brand();
        brand2.setId(1L);
        Brand brand3 = new Brand();
        brand3.setId(2L);

        assertEquals(brand1, brand2);
        assertNotEquals(brand1, brand3);
        assertEquals(Brand.class.hashCode(), brand1.hashCode());
    }

    @Test
    void product_option_equality_uses_id() {
        ProductOption option1 = new ProductOption();
        option1.setId(1L);
        ProductOption option2 = new ProductOption();
        option2.setId(1L);
        ProductOption option3 = new ProductOption();
        option3.setId(2L);

        assertEquals(option1, option2);
        assertNotEquals(option1, option3);
        assertEquals(ProductOption.class.hashCode(), option1.hashCode());
    }

    @Test
    void product_option_combination_equality_uses_id() {
        ProductOptionCombination combination1 = ProductOptionCombination.builder().id(1L).build();
        ProductOptionCombination combination2 = ProductOptionCombination.builder().id(1L).build();
        ProductOptionCombination combination3 = ProductOptionCombination.builder().id(2L).build();

        assertEquals(combination1, combination2);
        assertNotEquals(combination1, combination3);
        assertEquals(ProductOptionCombination.class.hashCode(), combination1.hashCode());
    }

    @Test
    void product_option_value_equality_uses_id() {
        ProductOptionValue value1 = ProductOptionValue.builder().id(1L).build();
        ProductOptionValue value2 = ProductOptionValue.builder().id(1L).build();
        ProductOptionValue value3 = ProductOptionValue.builder().id(2L).build();

        assertEquals(value1, value2);
        assertNotEquals(value1, value3);
        assertEquals(ProductOptionValue.class.hashCode(), value1.hashCode());
    }

    @Test
    void product_template_equality_uses_id() {
        ProductTemplate template1 = ProductTemplate.builder().id(1L).build();
        ProductTemplate template2 = ProductTemplate.builder().id(1L).build();
        ProductTemplate template3 = ProductTemplate.builder().id(2L).build();

        assertEquals(template1, template2);
        assertNotEquals(template1, template3);
        assertEquals(ProductTemplate.class.hashCode(), template1.hashCode());
    }

    @Test
    void product_attribute_equality_uses_id() {
        ProductAttribute attribute1 = ProductAttribute.builder().id(1L).build();
        ProductAttribute attribute2 = ProductAttribute.builder().id(1L).build();
        ProductAttribute attribute3 = ProductAttribute.builder().id(2L).build();

        assertEquals(attribute1, attribute2);
        assertNotEquals(attribute1, attribute3);
        assertEquals(ProductAttribute.class.hashCode(), attribute1.hashCode());
    }

    @Test
    void product_attribute_group_equality_uses_id() {
        ProductAttributeGroup group1 = new ProductAttributeGroup();
        group1.setId(1L);
        ProductAttributeGroup group2 = new ProductAttributeGroup();
        group2.setId(1L);
        ProductAttributeGroup group3 = new ProductAttributeGroup();
        group3.setId(2L);

        assertEquals(group1, group2);
        assertNotEquals(group1, group3);
        assertEquals(ProductAttributeGroup.class.hashCode(), group1.hashCode());
    }

    @Test
    void product_related_equality_uses_id() {
        ProductRelated related1 = ProductRelated.builder().id(1L).build();
        ProductRelated related2 = ProductRelated.builder().id(1L).build();
        ProductRelated related3 = ProductRelated.builder().id(2L).build();

        assertEquals(related1, related2);
        assertNotEquals(related1, related3);
        assertEquals(ProductRelated.class.hashCode(), related1.hashCode());
    }
}
