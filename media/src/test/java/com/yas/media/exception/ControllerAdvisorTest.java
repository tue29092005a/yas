package com.yas.media.exception;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.UnsupportedMediaTypeException;
import com.yas.media.controller.MediaController;
import com.yas.media.service.MediaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@ContextConfiguration(classes = {
    MediaController.class,
    ControllerAdvisor.class
})
@AutoConfigureMockMvc(addFilters = false)
class ControllerAdvisorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    @Test
    void handleNotFoundException_shouldReturnNotFound() throws Exception {
        when(mediaService.getMediaById(99L))
            .thenThrow(new NotFoundException("Media 99 is not found"));

        mockMvc.perform(get("/medias/{id}", 99L))
            .andExpect(status().isNotFound());
    }

    @Test
    void handleRuntimeException_shouldReturnInternalServerError() throws Exception {
        when(mediaService.getMediaById(1L))
            .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/medias/{id}", 1L))
            .andExpect(status().isInternalServerError());
    }
}
