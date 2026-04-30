package com.yas.search.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void testMessagesUtilsInstantiation() {
        new MessagesUtils();
    }

    @Test
    void getMessage_whenMessageCodeIsMissing_shouldReturnFormattedErrorCode() {
        String result = MessagesUtils.getMessage("missing.code", "Arg1", "Arg2");
        assertEquals("missing.code", result);
    }
    
    @Test
    void getMessage_whenMessageCodeExistsWithFormat_shouldReturnFormattedMessage() {
        String result = MessagesUtils.getMessage("missing.code {}", "Arg1");
        assertEquals("missing.code Arg1", result);
    }
}
