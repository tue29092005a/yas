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

import com.yas.tax.model.TaxRate;
import com.yas.tax.service.TaxRateService;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
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
@ContextConfiguration(classes = {TaxRateController.class})
@AutoConfigureMockMvc(addFilters = false)
class TaxRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxRateService taxRateService;

    @Nested
    class GetPageableTaxRatesTest {
        @Test
        void getPageableTaxRates_shouldReturnOk() throws Exception {
            TaxRateListGetVm listVm = new TaxRateListGetVm(
                List.of(new TaxRateGetDetailVm(1L, 10.0, "Zip", "Class", "State", "Country")), 1, 10, 1, 1, true);
            when(taxRateService.getPageableTaxRates(anyInt(), anyInt())).thenReturn(listVm);

            mockMvc.perform(get("/backoffice/tax-rates/paging")
                    .param("pageNo", "1")
                    .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxRateGetDetailContent[0].id").value(1L))
                .andExpect(jsonPath("$.taxRateGetDetailContent[0].rate").value(10.0))
                .andExpect(jsonPath("$.totalElements").value(1));

            verify(taxRateService, times(1)).getPageableTaxRates(1, 10);
        }
    }

    @Nested
    class GetTaxRateTest {
        @Test
        void getTaxRate_whenValidId_shouldReturnOk() throws Exception {
            TaxRateVm taxRateVm = new TaxRateVm(1L, 10.0, "Zip", 1L, 1L, 1L);
            when(taxRateService.findById(1L)).thenReturn(taxRateVm);

            mockMvc.perform(get("/backoffice/tax-rates/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.rate").value(10.0));

            verify(taxRateService, times(1)).findById(1L);
        }
    }

    @Nested
    class CreateTaxRateTest {
        @Test
        void createTaxRate_whenValidRequest_shouldReturnCreated() throws Exception {
            com.yas.tax.model.TaxClass taxClass = new com.yas.tax.model.TaxClass();
            taxClass.setId(1L);
            TaxRate taxRate = new TaxRate();
            taxRate.setId(1L);
            taxRate.setRate(10.0);
            taxRate.setTaxClass(taxClass);
            
            when(taxRateService.createTaxRate(any(TaxRatePostVm.class))).thenReturn(taxRate);

            String requestBody = """
                {
                    "rate": 10.0,
                    "taxClassId": 1,
                    "countryId": 1
                }
                """;

            mockMvc.perform(post("/backoffice/tax-rates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.rate").value(10.0));

            verify(taxRateService, times(1)).createTaxRate(any(TaxRatePostVm.class));
        }

        @Test
        void createTaxRate_whenInvalidRequest_shouldReturnBadRequest() throws Exception {
            String requestBody = """
                {
                    "rate": null
                }
                """;

            mockMvc.perform(post("/backoffice/tax-rates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class UpdateTaxRateTest {
        @Test
        void updateTaxRate_whenValidRequest_shouldReturnNoContent() throws Exception {
            doNothing().when(taxRateService).updateTaxRate(any(TaxRatePostVm.class), eq(1L));

            String requestBody = """
                {
                    "rate": 15.0,
                    "taxClassId": 1,
                    "countryId": 1
                }
                """;

            mockMvc.perform(put("/backoffice/tax-rates/{id}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNoContent());

            verify(taxRateService, times(1)).updateTaxRate(any(TaxRatePostVm.class), eq(1L));
        }
    }

    @Nested
    class DeleteTaxRateTest {
        @Test
        void deleteTaxRate_whenValidId_shouldReturnNoContent() throws Exception {
            doNothing().when(taxRateService).delete(1L);

            mockMvc.perform(delete("/backoffice/tax-rates/{id}", 1L))
                .andExpect(status().isNoContent());

            verify(taxRateService, times(1)).delete(1L);
        }
    }

    @Nested
    class GetTaxPercentByAddressTest {
        @Test
        void getTaxPercentByAddress_shouldReturnOk() throws Exception {
            when(taxRateService.getTaxPercent(1L, 2L, 3L, "12345")).thenReturn(10.5);

            mockMvc.perform(get("/backoffice/tax-rates/tax-percent")
                    .param("taxClassId", "1")
                    .param("countryId", "2")
                    .param("stateOrProvinceId", "3")
                    .param("zipCode", "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(10.5));

            verify(taxRateService, times(1)).getTaxPercent(1L, 2L, 3L, "12345");
        }
    }

    @Nested
    class GetBatchTaxPercentsByAddressTest {
        @Test
        void getBatchTaxPercentsByAddress_shouldReturnOk() throws Exception {
            List<TaxRateVm> taxRates = List.of(new TaxRateVm(1L, 10.5, "12345", 1L, 3L, 2L));
            when(taxRateService.getBulkTaxRate(List.of(1L, 4L), 2L, 3L, "12345")).thenReturn(taxRates);

            mockMvc.perform(get("/backoffice/tax-rates/location-based-batch")
                    .param("taxClassIds", "1", "4")
                    .param("countryId", "2")
                    .param("stateOrProvinceId", "3")
                    .param("zipCode", "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rate").value(10.5));

            verify(taxRateService, times(1)).getBulkTaxRate(List.of(1L, 4L), 2L, 3L, "12345");
        }
    }
}
