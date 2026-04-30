package com.yas.product.constants;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PageableConstantTest {

    @Test
    void constants_have_expected_defaults() {
        assertEquals("10", PageableConstant.DEFAULT_PAGE_SIZE);
        assertEquals("0", PageableConstant.DEFAULT_PAGE_NUMBER);
    }
}
