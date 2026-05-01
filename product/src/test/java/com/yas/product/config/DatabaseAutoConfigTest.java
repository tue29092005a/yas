package com.yas.product.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class DatabaseAutoConfigTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditor_aware_returns_empty_when_no_authentication() {
        AuditorAware<String> auditorAware = new DatabaseAutoConfig().auditorAware();

        assertEquals("", auditorAware.getCurrentAuditor().orElseThrow());
    }

    @Test
    void auditor_aware_returns_authentication_name() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("tester", "secret")
        );

        AuditorAware<String> auditorAware = new DatabaseAutoConfig().auditorAware();

        assertEquals("tester", auditorAware.getCurrentAuditor().orElseThrow());
    }
}
