package com.yas.tax.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.tax.model.TaxClass;
import com.yas.tax.service.TaxClassService;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@ContextConfiguration(classes = {TaxClassController.class})
@AutoConfigureMockMvc(addFilters = false)
class TaxClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxClassService taxClassService;

    @Nested
    class GetPageableTaxClassesTest {
        @Test
        void getPageableTaxClasses_shouldReturnOk() throws Exception {
            TaxClassListGetVm listVm = new TaxClassListGetVm(
                List.of(new TaxClassVm(1L, "Standard")), 1, 10, 1, 1, true);
            when(taxClassService.getPageableTaxClasses(anyInt(), anyInt())).thenReturn(listVm);

            mockMvc.perform(get("/backoffice/tax-classes/paging")
                    .param("pageNo", "1")
                    .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxClassContent[0].id").value(1L))
                .andExpect(jsonPath("$.taxClassContent[0].name").value("Standard"))
                .andExpect(jsonPath("$.totalElements").value(1));

            verify(taxClassService, times(1)).getPageableTaxClasses(1, 10);
        }
    }

    @Nested
    class ListTaxClassesTest {
        @Test
        void listTaxClasses_shouldReturnOk() throws Exception {
            List<TaxClassVm> taxClassVms = List.of(new TaxClassVm(1L, "Standard"));
            when(taxClassService.findAllTaxClasses()).thenReturn(taxClassVms);

            mockMvc.perform(get("/backoffice/tax-classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Standard"));

            verify(taxClassService, times(1)).findAllTaxClasses();
        }
    }

    @Nested
    class GetTaxClassTest {
        @Test
        void getTaxClass_whenValidId_shouldReturnOk() throws Exception {
            TaxClassVm taxClassVm = new TaxClassVm(1L, "Standard");
            when(taxClassService.findById(1L)).thenReturn(taxClassVm);

            mockMvc.perform(get("/backoffice/tax-classes/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Standard"));

            verify(taxClassService, times(1)).findById(1L);
        }
    }

    @Nested
    class CreateTaxClassTest {
        @Test
        void createTaxClass_whenValidRequest_shouldReturnCreated() throws Exception {
            TaxClass taxClass = new TaxClass();
            taxClass.setId(1L);
            taxClass.setName("Standard");
            
            when(taxClassService.create(any(TaxClassPostVm.class))).thenReturn(taxClass);

            String requestBody = """
                {
                    "id": "1",
                    "name": "Standard"
                }
                """;

            mockMvc.perform(post("/backoffice/tax-classes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Standard"));

            verify(taxClassService, times(1)).create(any(TaxClassPostVm.class));
        }

        @Test
        void createTaxClass_whenInvalidRequest_shouldReturnBadRequest() throws Exception {
            String requestBody = """
                {
                    "name": ""
                }
                """;

            mockMvc.perform(post("/backoffice/tax-classes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class UpdateTaxClassTest {
        @Test
        void updateTaxClass_whenValidRequest_shouldReturnNoContent() throws Exception {
            doNothing().when(taxClassService).update(any(TaxClassPostVm.class), eq(1L));

            String requestBody = """
                {
                    "id": "1",
                    "name": "Updated Standard"
                }
                """;

            mockMvc.perform(put("/backoffice/tax-classes/{id}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNoContent());

            verify(taxClassService, times(1)).update(any(TaxClassPostVm.class), eq(1L));
        }
    }

    @Nested
    class DeleteTaxClassTest {
        @Test
        void deleteTaxClass_whenValidId_shouldReturnNoContent() throws Exception {
            doNothing().when(taxClassService).delete(1L);

            mockMvc.perform(delete("/backoffice/tax-classes/{id}", 1L))
                .andExpect(status().isNoContent());

            verify(taxClassService, times(1)).delete(1L);
        }
    }
}
