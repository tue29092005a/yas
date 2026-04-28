package com.yas.product.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProductConverterTest {

    @Test
    void toSlug_trims_and_normalizes_input() {
        String result = ProductConverter.toSlug("  Hello, World!  ");

        assertEquals("hello-world-", result);
    }

    @Test
    void toSlug_removes_leading_hyphen() {
        String result = ProductConverter.toSlug("---ABC");

        assertEquals("abc", result);
    }
}
