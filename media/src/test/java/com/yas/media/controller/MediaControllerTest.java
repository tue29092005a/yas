package com.yas.media.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.media.exception.ControllerAdvisor;
import com.yas.media.model.Media;
import com.yas.media.service.MediaService;
import com.yas.media.viewmodel.MediaVm;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
// ts32f2 test ci 
@WebMvcTest(excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@ContextConfiguration(classes = {
    MediaController.class,
    ControllerAdvisor.class,
    MediaControllerTest.NoOpValidatorConfig.class
})
@AutoConfigureMockMvc(addFilters = false)
class MediaControllerTest {

    /**
     * Replace Jakarta Bean Validation Validator with a Mockito mock that returns empty violation
     * sets, so @ValidFileType (which calls ImageIO.read) is NOT invoked during controller tests.
     * Validation logic is tested separately in FileTypeValidatorTest.
     */
    @TestConfiguration
    static class NoOpValidatorConfig {
        @Bean
        Validator validator() {
            Validator validatorMock = mock(Validator.class);
            ExecutableValidator execMock = mock(ExecutableValidator.class);

            when(validatorMock.validate(any(), any())).thenReturn(Set.of());
            when(validatorMock.validateProperty(any(), any(), any())).thenReturn(Set.of());
            when(validatorMock.validateValue(any(), any(), any(), any())).thenReturn(Set.of());
            when(validatorMock.forExecutables()).thenReturn(execMock);

            when(execMock.validateParameters(any(), any(), any(), any())).thenReturn(Set.of());
            when(execMock.validateReturnValue(any(), any(), any(), any())).thenReturn(Set.of());
            when(execMock.validateConstructorParameters(any(), any(), any())).thenReturn(Set.of());
            when(execMock.validateConstructorReturnValue(any(), any(), any())).thenReturn(Set.of());

            return validatorMock;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    // ===================== POST /medias =====================
    @Nested
    class CreateMediaTest {

        @Test
        void testCreate_whenPngFileProvided_shouldReturnOk() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "multipartFile", "image.png", "image/png", new byte[]{1, 2, 3}
            );
            Media savedMedia = buildMedia(1L, "test-caption", "image.png", "image/png");
            when(mediaService.saveMedia(any())).thenReturn(savedMedia);

            mockMvc.perform(multipart("/medias").file(file).param("caption", "test-caption"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.caption").value("test-caption"))
                .andExpect(jsonPath("$.fileName").value("image.png"));

            verify(mediaService, times(1)).saveMedia(any());
        }

        @Test
        void testCreate_whenJpegFileProvided_shouldReturnOk() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "multipartFile", "photo.jpeg", "image/jpeg", new byte[]{10, 20, 30}
            );
            Media savedMedia = buildMedia(2L, "jpeg-caption", "photo.jpeg", "image/jpeg");
            when(mediaService.saveMedia(any())).thenReturn(savedMedia);

            mockMvc.perform(multipart("/medias").file(file).param("caption", "jpeg-caption"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.caption").value("jpeg-caption"));

            verify(mediaService, times(1)).saveMedia(any());
        }

        @Test
        void testCreate_whenGifFileProvided_shouldReturnOk() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "multipartFile", "anim.gif", "image/gif", new byte[]{0x47, 0x49, 0x46}
            );
            Media savedMedia = buildMedia(3L, "gif-caption", "anim.gif", "image/gif");
            when(mediaService.saveMedia(any())).thenReturn(savedMedia);

            mockMvc.perform(multipart("/medias").file(file).param("caption", "gif-caption"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L));

            verify(mediaService, times(1)).saveMedia(any());
        }

        @Test
        void testCreate_whenFileIsMissing_shouldReturnError() throws Exception {
            // No multipart file part: with no-op validator, @NotNull is skipped,
            // so the null file reaches the service which throws → handled as 4xx/5xx
            when(mediaService.saveMedia(any()))
                .thenThrow(new RuntimeException("multipartFile is null"));

            mockMvc.perform(multipart("/medias").param("caption", "no-file"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                        status >= 400, "Expected error response but was: " + status);
                });
        }
    }

    // ===================== DELETE /medias/{id} =====================
    @Nested
    class DeleteMediaTest {

        @Test
        void testDelete_whenValidId_shouldReturnNoContent() throws Exception {
            doNothing().when(mediaService).removeMedia(1L);

            mockMvc.perform(delete("/medias/{id}", 1L))
                .andExpect(status().isNoContent());

            verify(mediaService, times(1)).removeMedia(1L);
        }

        @Test
        void testDelete_whenMediaNotFound_shouldReturnNotFound() throws Exception {
            org.mockito.Mockito.doThrow(
                new com.yas.commonlibrary.exception.NotFoundException("Media 99 is not found")
            ).when(mediaService).removeMedia(99L);

            mockMvc.perform(delete("/medias/{id}", 99L))
                .andExpect(status().isNotFound());
        }
    }

    // ===================== GET /medias/{id} =====================
    @Nested
    class GetMediaByIdTest {

        @Test
        void testGet_whenMediaExists_shouldReturnOk() throws Exception {
            MediaVm mediaVm = new MediaVm(1L, "caption", "image.png", "image/png",
                "http://localhost/medias/1/file/image.png");
            when(mediaService.getMediaById(1L)).thenReturn(mediaVm);

            mockMvc.perform(get("/medias/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.caption").value("caption"))
                .andExpect(jsonPath("$.fileName").value("image.png"))
                .andExpect(jsonPath("$.url").value("http://localhost/medias/1/file/image.png"));

            verify(mediaService, times(1)).getMediaById(1L);
        }

        @Test
        void testGet_whenMediaNotFound_shouldReturnNotFound() throws Exception {
            when(mediaService.getMediaById(99L)).thenReturn(null);

            mockMvc.perform(get("/medias/{id}", 99L))
                .andExpect(status().isNotFound());

            verify(mediaService, times(1)).getMediaById(99L);
        }
    }

    // ===================== GET /medias?ids=... =====================
    @Nested
    class GetMediaByIdsTest {

        @Test
        void testGetByIds_whenMediasExist_shouldReturnOk() throws Exception {
            List<MediaVm> mediaVms = List.of(
                new MediaVm(1L, "caption1", "img1.png", "image/png",
                    "http://localhost/medias/1/file/img1.png"),
                new MediaVm(2L, "caption2", "img2.jpg", "image/jpeg",
                    "http://localhost/medias/2/file/img2.jpg")
            );
            when(mediaService.getMediaByIds(anyList())).thenReturn(mediaVms);

            mockMvc.perform(get("/medias").param("ids", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

            verify(mediaService, times(1)).getMediaByIds(anyList());
        }

        @Test
        void testGetByIds_whenResultIsEmpty_shouldReturnNotFound() throws Exception {
            when(mediaService.getMediaByIds(anyList())).thenReturn(List.of());

            mockMvc.perform(get("/medias").param("ids", "99"))
                .andExpect(status().isNotFound());
        }

        @Test
        void testGetByIds_whenIdsParamMissing_shouldReturn4xx() throws Exception {
            mockMvc.perform(get("/medias"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                        status >= 400, "Expected 4xx or 5xx but was: " + status);
                });
        }
    }

    // ===================== GET /medias/{id}/file/{fileName} =====================
    @Nested
    class GetFileTest {

        @Test
        void testGetFile_whenFileExists_shouldReturnOkWithContent() throws Exception {
            byte[] content = "fake-image-bytes".getBytes();
            java.io.InputStream inputStream = new java.io.ByteArrayInputStream(content);

            com.yas.media.model.dto.MediaDto mediaDto = com.yas.media.model.dto.MediaDto.builder()
                .content(inputStream)
                .mediaType(org.springframework.http.MediaType.IMAGE_PNG)
                .build();

            when(mediaService.getFile(1L, "image.png")).thenReturn(mediaDto);

            mockMvc.perform(get("/medias/{id}/file/{fileName}", 1L, "image.png"))
                .andExpect(status().isOk());

            verify(mediaService, times(1)).getFile(1L, "image.png");
        }

        @Test
        void testGetFile_withJpegContent_shouldReturnOk() throws Exception {
            byte[] content = "jpeg-data".getBytes();
            com.yas.media.model.dto.MediaDto mediaDto = com.yas.media.model.dto.MediaDto.builder()
                .content(new java.io.ByteArrayInputStream(content))
                .mediaType(org.springframework.http.MediaType.IMAGE_JPEG)
                .build();

            when(mediaService.getFile(2L, "photo.jpg")).thenReturn(mediaDto);

            mockMvc.perform(get("/medias/{id}/file/{fileName}", 2L, "photo.jpg"))
                .andExpect(status().isOk());

            verify(mediaService, times(1)).getFile(2L, "photo.jpg");
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Media buildMedia(Long id, String caption, String fileName, String mediaType) {
        Media m = new Media();
        m.setId(id);
        m.setCaption(caption);
        m.setFileName(fileName);
        m.setMediaType(mediaType);
        return m;
    }
}
