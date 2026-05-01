package com.yas.product.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yas.product.model.ProductVariationSaveVm;
import com.yas.product.viewmodel.error.ErrorVm;
import com.yas.product.viewmodel.product.ProductSaveVm;
import com.yas.product.viewmodel.productattribute.ProductAttributeGroupGetVm;
import com.yas.product.viewmodel.productattribute.ProductAttributeValueVm;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ViewModelTest {

    @Test
    void product_attribute_group_get_vm_exposes_fields() {
        ProductAttributeValueVm valueVm = new ProductAttributeValueVm("Color", "Red");
        ProductAttributeGroupGetVm vm = new ProductAttributeGroupGetVm("Specs", List.of(valueVm));

        assertEquals("Specs", vm.name());
        assertEquals("Color", vm.productAttributeValues().get(0).name());
    }

    @Test
    void error_vm_constructs_with_default_field_errors() {
        ErrorVm vm = new ErrorVm("400", "Bad Request", "Detail");

        assertEquals("400", vm.statusCode());
        assertTrue(vm.fieldErrors().isEmpty());
    }

    @Test
    void product_save_vm_default_id_is_null() {
        DummyProductSaveVm vm = new DummyProductSaveVm();

        assertNull(vm.id());
        assertEquals("Name", vm.name());
    }

    private static class DummyProductSaveVm implements ProductSaveVm<DummyVariation> {
        @Override
        public List<DummyVariation> variations() {
            return List.of();
        }

        @Override
        public Boolean isPublished() {
            return true;
        }

        @Override
        public Double length() {
            return 10.0;
        }

        @Override
        public Double width() {
            return 5.0;
        }

        @Override
        public String name() {
            return "Name";
        }

        @Override
        public String slug() {
            return "slug";
        }

        @Override
        public String sku() {
            return "sku";
        }

        @Override
        public String gtin() {
            return "gtin";
        }

        @Override
        public Double price() {
            return 10.0;
        }

        @Override
        public Long thumbnailMediaId() {
            return 1L;
        }

        @Override
        public List<Long> productImageIds() {
            return List.of(2L);
        }
    }

    private static class DummyVariation implements ProductVariationSaveVm {
        @Override
        public Map<Long, String> optionValuesByOptionId() {
            return Map.of(1L, "M");
        }

        @Override
        public Long id() {
            return 1L;
        }

        @Override
        public String name() {
            return "Var";
        }

        @Override
        public String slug() {
            return "var";
        }

        @Override
        public String sku() {
            return "sku";
        }

        @Override
        public String gtin() {
            return "gtin";
        }

        @Override
        public Double price() {
            return 2.0;
        }

        @Override
        public Long thumbnailMediaId() {
            return 1L;
        }

        @Override
        public List<Long> productImageIds() {
            return List.of(2L);
        }
    }
}
