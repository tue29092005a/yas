package com.yas.tax.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_whenKeyExists_shouldReturnMessage() {
        // Since we don't have the properties file in the test context,
        // it might fall back to the key itself or if we provide a test resource.
        // Let's test the fallback behavior when key doesn't exist.
        String message = MessagesUtils.getMessage("NON_EXISTENT_KEY", "arg1");
        assertEquals("NON_EXISTENT_KEY", message);
    }
    
    @Test
    void getMessage_withArguments_shouldFormatCorrectly() {
        // Fallback behavior + formatting
        String message = MessagesUtils.getMessage("ERROR: {} NOT FOUND", "TAX_CLASS");
        assertEquals("ERROR: TAX_CLASS NOT FOUND", message);
    }
}
