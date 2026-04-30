package com.yas.search.viewmodel.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void testErrorVmConstructorsAndAccessors() {
        ErrorVm error1 = new ErrorVm("400", "Bad Request", "Error message");
        assertEquals("400", error1.statusCode());
        assertEquals("Bad Request", error1.title());
        assertEquals("Error message", error1.detail());
        assertTrue(error1.fieldErrors().isEmpty());

        ErrorVm error2 = new ErrorVm("400", "Bad Request", "Error message", List.of("field1"));
        assertEquals("400", error2.statusCode());
        assertEquals("Bad Request", error2.title());
        assertEquals("Error message", error2.detail());
        assertEquals(1, error2.fieldErrors().size());
        assertEquals("field1", error2.fieldErrors().get(0));
    }
}
