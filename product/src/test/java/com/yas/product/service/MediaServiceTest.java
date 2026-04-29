package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
// force rebuild
import com.yas.commonlibrary.config.ServiceUrlConfig;
import com.yas.product.viewmodel.NoFileMediaVm;
import java.net.URI;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

class MediaServiceTest {

    @Test
    void getMedia_whenIdNull_returnsEmptyVm() {
        RestClient restClient = Mockito.mock(RestClient.class);
        ServiceUrlConfig serviceUrlConfig = Mockito.mock(ServiceUrlConfig.class);
        MediaService mediaService = new MediaService(restClient, serviceUrlConfig);

        NoFileMediaVm result = mediaService.getMedia(null);

        assertThat(result.id()).isNull();
        assertThat(result.url()).isEmpty();
    }

    @Test
    void getMedia_whenIdProvided_returnsMedia() {
        RestClient restClient = Mockito.mock(RestClient.class);
        @SuppressWarnings("rawtypes")
        RestClient.RequestHeadersUriSpec getSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        @SuppressWarnings("rawtypes")
        RestClient.RequestHeadersSpec headersSpec = Mockito.mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        ServiceUrlConfig serviceUrlConfig = Mockito.mock(ServiceUrlConfig.class);
        Mockito.when(serviceUrlConfig.media()).thenReturn("http://media");
        Mockito.when(restClient.get()).thenReturn(getSpec);
        Mockito.when(getSpec.uri(any(URI.class))).thenReturn(headersSpec);
        Mockito.when(headersSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.body(NoFileMediaVm.class))
                .thenReturn(new NoFileMediaVm(1L, "", "", "", "url-1"));

        MediaService mediaService = new MediaService(restClient, serviceUrlConfig);

        NoFileMediaVm result = mediaService.getMedia(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.url()).isEqualTo("url-1");
    }

    @Test
    void saveFile_usesJwtAndReturnsMedia() {
        RestClient restClient = Mockito.mock(RestClient.class);
        @SuppressWarnings("rawtypes")
        RestClient.RequestBodyUriSpec postSpec = Mockito.mock(RestClient.RequestBodyUriSpec.class);
        @SuppressWarnings("rawtypes")
        RestClient.RequestBodySpec bodySpec = Mockito.mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        ServiceUrlConfig serviceUrlConfig = Mockito.mock(ServiceUrlConfig.class);
        Mockito.when(serviceUrlConfig.media()).thenReturn("http://media");
        Mockito.when(restClient.post()).thenReturn(postSpec);
        Mockito.when(postSpec.uri(any(URI.class))).thenReturn(bodySpec);
        Mockito.when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
        Mockito.when(bodySpec.headers(any(Consumer.class))).thenReturn(bodySpec);
        Mockito.when(bodySpec.body(Mockito.any(Object.class))).thenReturn(bodySpec);
        Mockito.when(bodySpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.body(NoFileMediaVm.class)).thenReturn(new NoFileMediaVm(2L, "", "", "", "url-2"));

        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "test").build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        try {
            MediaService mediaService = new MediaService(restClient, serviceUrlConfig);
            MultipartFile file = new MockMultipartFile("file", "name.txt", "text/plain", "data".getBytes());

            NoFileMediaVm result = mediaService.saveFile(file, "cap", "name");

            assertThat(result.id()).isEqualTo(2L);
            assertThat(result.url()).isEqualTo("url-2");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void removeMedia_usesJwtAndCompletes() {
        RestClient restClient = Mockito.mock(RestClient.class);
        @SuppressWarnings("rawtypes")
        RestClient.RequestHeadersUriSpec deleteSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        @SuppressWarnings("rawtypes")
        RestClient.RequestHeadersSpec headersSpec = Mockito.mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        ServiceUrlConfig serviceUrlConfig = Mockito.mock(ServiceUrlConfig.class);
        Mockito.when(serviceUrlConfig.media()).thenReturn("http://media");
        Mockito.when(restClient.delete()).thenReturn(deleteSpec);
        Mockito.when(deleteSpec.uri(any(URI.class))).thenReturn(headersSpec);
        Mockito.when(headersSpec.headers(any())).thenReturn(headersSpec);
        Mockito.when(headersSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.body(Void.class)).thenReturn(null);

        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "test").build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        try {
            MediaService mediaService = new MediaService(restClient, serviceUrlConfig);

            assertDoesNotThrow(() -> mediaService.removeMedia(3L));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
