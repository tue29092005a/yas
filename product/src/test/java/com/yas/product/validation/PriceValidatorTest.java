package com.yas.product.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PriceValidatorTest {

    private final PriceValidator priceValidator = new PriceValidator();

    @Test
    void isValid_returns_true_for_zero_and_positive() {
        assertTrue(priceValidator.isValid(0.0, null));
        assertTrue(priceValidator.isValid(10.5, null));
    }

    @Test
    void isValid_returns_false_for_negative() {
        assertFalse(priceValidator.isValid(-1.0, null));
    }
}
