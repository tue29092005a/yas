package com.yas.tax.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void instantiateConstants_shouldBePossible() {
        Constants constants = new Constants();
        assertNotNull(constants);
    }
}
