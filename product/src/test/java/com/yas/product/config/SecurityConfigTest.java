package com.yas.product.config;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

class SecurityConfigTest {

    @Test
    void filter_chain_is_created() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);

        when(http.authorizeHttpRequests(org.mockito.Mockito.any())).thenReturn(http);
        when(http.oauth2ResourceServer(org.mockito.Mockito.any())).thenReturn(http);
        when(http.build()).thenReturn(chain);

        SecurityFilterChain result = new SecurityConfig().filterChain(http);

        assertSame(chain, result);
        verify(http).authorizeHttpRequests(org.mockito.Mockito.any());
        verify(http).oauth2ResourceServer(org.mockito.Mockito.any());
        verify(http).build();
    }

    @Test
    void jwt_converter_maps_roles_with_prefix() {
        JwtAuthenticationConverter converter = new SecurityConfig().jwtAuthenticationConverterForKeycloak();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "USER")))
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt);
        Set<String> authorities = toAuthoritySet(token.getAuthorities());

        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("ROLE_USER"));
    }

    private Set<String> toAuthoritySet(Collection<GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
