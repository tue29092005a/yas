package com.yas.product.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void error_codes_are_defined() {
        assertEquals("PRODUCT_NOT_FOUND", Constants.ErrorCode.PRODUCT_NOT_FOUND);
        assertNotNull(Constants.ErrorCode.NAME_ALREADY_EXITED);
        assertNotNull(Constants.ErrorCode.MAKE_SURE_LENGTH_GREATER_THAN_WIDTH);
    }
}
