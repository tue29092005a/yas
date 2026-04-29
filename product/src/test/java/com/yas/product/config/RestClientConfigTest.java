package com.yas.product.config;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class RestClientConfigTest {

    @Test
    void rest_client_builder_sets_default_content_type() {
        RestClient.Builder builder = org.mockito.Mockito.mock(RestClient.Builder.class);
        RestClient restClient = org.mockito.Mockito.mock(RestClient.class);

        when(builder.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        RestClientConfig config = new RestClientConfig();
        RestClient result = config.getRestClient(builder);

        assertSame(restClient, result);
        verify(builder).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        verify(builder).build();
    }
}
