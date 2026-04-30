package com.yas.product.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class SwaggerConfigTest {

    @Test
    void swagger_config_has_openapi_definition() {
        OpenAPIDefinition definition = SwaggerConfig.class.getAnnotation(OpenAPIDefinition.class);

        assertNotNull(definition);
        assertEquals("Product Service API", definition.info().title());
        assertEquals("1.0", definition.info().version());
    }

    @Test
    void swagger_config_has_security_scheme() {
        SecurityScheme scheme = SwaggerConfig.class.getAnnotation(SecurityScheme.class);

        assertNotNull(scheme);
        assertEquals("oauth2_bearer", scheme.name());
    }
}
