package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    @Test
    void handleTypedFallback_rethrows() {
        TestHandler handler = new TestHandler();
        RuntimeException exception = new RuntimeException("boom");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> handler.callTyped(exception));

        assertSame(exception, thrown);
    }

    @Test
    void handleBodilessFallback_rethrows() {
        TestHandler handler = new TestHandler();
        IllegalStateException exception = new IllegalStateException("boom");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> handler.callBodiless(exception));

        assertSame(exception, thrown);
    }

    private static class TestHandler extends AbstractCircuitBreakFallbackHandler {
        void callBodiless(Throwable throwable) throws Throwable {
            handleBodilessFallback(throwable);
        }

        <T> T callTyped(Throwable throwable) throws Throwable {
            return handleTypedFallback(throwable);
        }
    }
}
