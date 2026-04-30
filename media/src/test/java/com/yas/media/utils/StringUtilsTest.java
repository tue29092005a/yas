package com.yas.media.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StringUtilsTest {

    @Nested
    class HasTextTest {

        @Test
        void hasText_whenInputIsNull_shouldReturnFalse() {
            assertFalse(StringUtils.hasText(null));
        }

        @Test
        void hasText_whenInputIsEmpty_shouldReturnFalse() {
            assertFalse(StringUtils.hasText(""));
        }

        @Test
        void hasText_whenInputIsBlank_shouldReturnFalse() {
            assertFalse(StringUtils.hasText("   "));
        }

        @Test
        void hasText_whenInputHasText_shouldReturnTrue() {
            assertTrue(StringUtils.hasText("hello"));
        }

        @Test
        void hasText_whenInputHasTextWithSpaces_shouldReturnTrue() {
            assertTrue(StringUtils.hasText("  hello  "));
        }

        @Test
        void hasText_whenInputIsSingleChar_shouldReturnTrue() {
            assertTrue(StringUtils.hasText("a"));
        }
    }
}
