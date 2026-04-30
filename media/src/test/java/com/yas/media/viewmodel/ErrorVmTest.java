package com.yas.media.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void errorVm_threeArgConstructor_shouldSetFieldErrorsToEmptyList() {
        ErrorVm errorVm = new ErrorVm("400 BAD_REQUEST", "Bad Request", "Invalid input");

        assertEquals("400 BAD_REQUEST", errorVm.statusCode());
        assertEquals("Bad Request", errorVm.title());
        assertEquals("Invalid input", errorVm.detail());
        assertNotNull(errorVm.fieldErrors());
        assertTrue(errorVm.fieldErrors().isEmpty());
    }

    @Test
    void errorVm_fourArgConstructor_shouldSetAllFields() {
        List<String> errors = List.of("field1 is required", "field2 must be positive");
        ErrorVm errorVm = new ErrorVm("400 BAD_REQUEST", "Bad Request", "Validation failed", errors);

        assertEquals("400 BAD_REQUEST", errorVm.statusCode());
        assertEquals("Bad Request", errorVm.title());
        assertEquals("Validation failed", errorVm.detail());
        assertEquals(2, errorVm.fieldErrors().size());
        assertEquals("field1 is required", errorVm.fieldErrors().get(0));
    }

    @Test
    void errorVm_threeArgConstructor_fieldErrorsShouldBeMutable() {
        ErrorVm errorVm = new ErrorVm("500", "Error", "Something went wrong");
        // Ensure the ArrayList returned is mutable
        errorVm.fieldErrors().add("new error");
        assertEquals(1, errorVm.fieldErrors().size());
    }
}
