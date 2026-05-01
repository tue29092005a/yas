package com.yas.search.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

class ProductFieldTest {

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<ProductField> constructor = ProductField.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    void testProductFieldConstants() {
        assertEquals("name", ProductField.NAME);
        assertEquals("brand", ProductField.BRAND);
        assertEquals("price", ProductField.PRICE);
        assertEquals("categories", ProductField.CATEGORIES);
        assertEquals("attributes", ProductField.ATTRIBUTES);
        assertEquals("isPublished", ProductField.IS_PUBLISHED);
        assertEquals("createdOn", ProductField.CREATE_ON);
    }
}
