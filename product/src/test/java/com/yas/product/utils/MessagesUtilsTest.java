package com.yas.product.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_returns_formatted_message() {
        String result = MessagesUtils.getMessage("NAME_ALREADY_EXITED", "Phone");

        assertEquals("Request name Phone is already existed", result);
    }

    @Test
    void getMessage_returns_code_when_missing() {
        String result = MessagesUtils.getMessage("UNKNOWN_CODE");

        assertEquals("UNKNOWN_CODE", result);
    }
}
