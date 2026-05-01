package com.yas.tax.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViewModelTest {

    @Test
    void testTaxRatePostVm() {
        TaxRatePostVm vm = new TaxRatePostVm(10.0, "12345", 1L, 2L, 3L);
        assertEquals(10.0, vm.rate());
        assertEquals("12345", vm.zipCode());
        assertEquals(1L, vm.taxClassId());
        assertEquals(2L, vm.stateOrProvinceId());
        assertEquals(3L, vm.countryId());
    }

    @Test
    void testTaxRateGetDetailVm() {
        TaxRateGetDetailVm vm = new TaxRateGetDetailVm(1L, 10.0, "12345", "Class", "State", "Country");
        assertEquals(1L, vm.id());
        assertEquals(10.0, vm.rate());
        assertEquals("12345", vm.zipCode());
        assertEquals("Class", vm.taxClassName());
        assertEquals("State", vm.stateOrProvinceName());
        assertEquals("Country", vm.countryName());
    }

    @Test
    void testTaxRateListGetVm() {
        TaxRateGetDetailVm detailVm = new TaxRateGetDetailVm(1L, 10.0, "12345", "Class", "State", "Country");
        TaxRateListGetVm vm = new TaxRateListGetVm(List.of(detailVm), 1, 10, 1, 1, true);
        assertEquals(1, vm.taxRateGetDetailContent().size());
        assertEquals(1, vm.pageNo());
        assertEquals(10, vm.pageSize());
        assertEquals(1, vm.totalElements());
        assertEquals(1, vm.totalPages());
        assertTrue(vm.isLast());
    }
    
    private void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }

    @Test
    void testTaxRateVm() {
        TaxRateVm vm = new TaxRateVm(1L, 10.0, "12345", 3L, 2L, 1L);
        assertEquals(1L, vm.id());
        assertEquals(10.0, vm.rate());
        assertEquals(1L, vm.countryId());
        assertEquals(2L, vm.stateOrProvinceId());
        assertEquals("12345", vm.zipCode());
        assertEquals(3L, vm.taxClassId());
    }

    @Test
    void testTaxClassPostVm() {
        TaxClassPostVm vm = new TaxClassPostVm("1", "Standard");
        assertEquals("1", vm.id());
        assertEquals("Standard", vm.name());
    }

    @Test
    void testTaxClassListGetVm() {
        TaxClassVm classVm = new TaxClassVm(1L, "Standard");
        TaxClassListGetVm vm = new TaxClassListGetVm(List.of(classVm), 1, 10, 1, 1, true);
        assertEquals(1, vm.taxClassContent().size());
        assertEquals(1, vm.pageNo());
        assertEquals(10, vm.pageSize());
        assertEquals(1, vm.totalElements());
        assertEquals(1, vm.totalPages());
    }

    @Test
    void testStateOrProvinceAndCountryGetNameVm() {
        StateOrProvinceAndCountryGetNameVm vm = new StateOrProvinceAndCountryGetNameVm(1L, "State", "Country");
        assertEquals("State", vm.stateOrProvinceName());
        assertEquals("Country", vm.countryName());
    }
}
