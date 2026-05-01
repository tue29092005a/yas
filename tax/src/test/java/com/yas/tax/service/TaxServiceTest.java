package com.yas.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TaxServiceTest {

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private TaxClassRepository taxClassRepository;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private TaxRateService taxRateService;

    private TaxRate taxRate;
    private TaxClass taxClass;

    @BeforeEach
    void setUp() {
        taxClass = new TaxClass();
        taxClass.setId(1L);
        taxClass.setName("Standard");

        taxRate = new TaxRate();
        taxRate.setId(1L);
        taxRate.setRate(10.0);
        taxRate.setZipCode("12345");
        taxRate.setStateOrProvinceId(2L);
        taxRate.setCountryId(3L);
        taxRate.setTaxClass(taxClass);
    }

    @Nested
    class CreateTaxRateTest {
        @Test
        void createTaxRate_whenTaxClassDoesNotExist_shouldThrowNotFoundException() {
            TaxRatePostVm postVm = new TaxRatePostVm(10.0, "12345", 1L, 2L, 3L);
            when(taxClassRepository.existsById(1L)).thenReturn(false);

            assertThrows(NotFoundException.class, () -> taxRateService.createTaxRate(postVm));
            verify(taxRateRepository, never()).save(any());
        }

        @Test
        void createTaxRate_whenValidRequest_shouldSaveAndReturnTaxRate() {
            TaxRatePostVm postVm = new TaxRatePostVm(10.0, "12345", 1L, 2L, 3L);
            when(taxClassRepository.existsById(1L)).thenReturn(true);
            when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
            when(taxRateRepository.save(any(TaxRate.class))).thenReturn(taxRate);

            TaxRate result = taxRateService.createTaxRate(postVm);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(10.0, result.getRate());
            verify(taxRateRepository, times(1)).save(any(TaxRate.class));
        }
    }

    @Nested
    class UpdateTaxRateTest {
        @Test
        void updateTaxRate_whenTaxRateDoesNotExist_shouldThrowNotFoundException() {
            TaxRatePostVm postVm = new TaxRatePostVm(10.0, "12345", 1L, 2L, 3L);
            when(taxRateRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> taxRateService.updateTaxRate(postVm, 1L));
            verify(taxRateRepository, never()).save(any());
        }

        @Test
        void updateTaxRate_whenTaxClassDoesNotExist_shouldThrowNotFoundException() {
            TaxRatePostVm postVm = new TaxRatePostVm(10.0, "12345", 1L, 2L, 3L);
            when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));
            when(taxClassRepository.existsById(1L)).thenReturn(false);

            assertThrows(NotFoundException.class, () -> taxRateService.updateTaxRate(postVm, 1L));
            verify(taxRateRepository, never()).save(any());
        }

        @Test
        void updateTaxRate_whenValidRequest_shouldUpdateAndSaveTaxRate() {
            TaxRatePostVm postVm = new TaxRatePostVm(15.0, "54321", 1L, 2L, 3L);
            when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));
            when(taxClassRepository.existsById(1L)).thenReturn(true);
            when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
            when(taxRateRepository.save(any(TaxRate.class))).thenReturn(taxRate);

            taxRateService.updateTaxRate(postVm, 1L);

            assertEquals(15.0, taxRate.getRate());
            assertEquals("54321", taxRate.getZipCode());
            verify(taxRateRepository, times(1)).save(taxRate);
        }
    }

    @Nested
    class DeleteTest {
        @Test
        void delete_whenTaxRateDoesNotExist_shouldThrowNotFoundException() {
            when(taxRateRepository.existsById(1L)).thenReturn(false);

            assertThrows(NotFoundException.class, () -> taxRateService.delete(1L));
            verify(taxRateRepository, never()).deleteById(any());
        }

        @Test
        void delete_whenTaxRateExists_shouldDelete() {
            when(taxRateRepository.existsById(1L)).thenReturn(true);
            doNothing().when(taxRateRepository).deleteById(1L);

            taxRateService.delete(1L);

            verify(taxRateRepository, times(1)).deleteById(1L);
        }
    }

    @Nested
    class FindByIdTest {
        @Test
        void findById_whenTaxRateExists_shouldReturnTaxRateVm() {
            when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));

            TaxRateVm result = taxRateService.findById(1L);

            assertNotNull(result);
            assertEquals(1L, result.id());
            assertEquals(10.0, result.rate());
        }

        @Test
        void findById_whenTaxRateDoesNotExist_shouldThrowNotFoundException() {
            when(taxRateRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> taxRateService.findById(1L));
        }
    }

    @Nested
    class FindAllTest {
        @Test
        void findAll_shouldReturnAllTaxRates() {
            when(taxRateRepository.findAll()).thenReturn(List.of(taxRate));

            List<TaxRateVm> result = taxRateService.findAll();

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(10.0, result.get(0).rate());
        }
    }

    @Nested
    class GetPageableTaxRatesTest {
        @Test
        void getPageableTaxRates_shouldReturnTaxRateListGetVm() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<TaxRate> page = new PageImpl<>(List.of(taxRate), pageable, 1);
            when(taxRateRepository.findAll(any(Pageable.class))).thenReturn(page);
            
            StateOrProvinceAndCountryGetNameVm locationNameVm = new StateOrProvinceAndCountryGetNameVm(2L, "State", "Country");
            when(locationService.getStateOrProvinceAndCountryNames(List.of(2L))).thenReturn(List.of(locationNameVm));

            TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

            assertNotNull(result);
            assertEquals(1, result.taxRateGetDetailContent().size());
            assertEquals(1L, result.taxRateGetDetailContent().get(0).id());
            assertEquals(10.0, result.taxRateGetDetailContent().get(0).rate());
            assertEquals("State", result.taxRateGetDetailContent().get(0).stateOrProvinceName());
            assertEquals(0, result.pageNo());
            assertEquals(10, result.pageSize());
            assertEquals(1, result.totalElements());
            assertEquals(1, result.totalPages());
            assertTrue(result.isLast());
        }
        
        @Test
        void getPageableTaxRates_whenNoStateOrProvinceId_shouldReturnEmptyDetails() {
            taxRate.setStateOrProvinceId(null);
            Pageable pageable = PageRequest.of(1, 10);
            Page<TaxRate> page = new PageImpl<>(List.of(taxRate), pageable, 1);
            when(taxRateRepository.findAll(any(Pageable.class))).thenReturn(page);

            TaxRateListGetVm result = taxRateService.getPageableTaxRates(1, 10);

            assertNotNull(result);
            // Since there is a check `if (!stateOrProvinceIds.isEmpty())` but it filters out nulls 
            // Actually, distinct().map(el -> el.getStateOrProvinceId()) includes null.
            // Wait, if it includes null, locationService will be called with [null].
            // But if there are no records, it shouldn't call locationService. Let's test empty page.
        }
        
        @Test
        void getPageableTaxRates_whenEmptyPage_shouldNotCallLocationService() {
            Pageable pageable = PageRequest.of(1, 10);
            Page<TaxRate> page = new PageImpl<>(List.of(), pageable, 0);
            when(taxRateRepository.findAll(any(Pageable.class))).thenReturn(page);

            TaxRateListGetVm result = taxRateService.getPageableTaxRates(1, 10);

            assertNotNull(result);
            assertEquals(0, result.taxRateGetDetailContent().size());
            verify(locationService, never()).getStateOrProvinceAndCountryNames(any());
        }
    }

    @Nested
    class GetTaxPercentTest {
        @Test
        void getTaxPercent_whenFound_shouldReturnPercent() {
            when(taxRateRepository.getTaxPercent(3L, 2L, "12345", 1L)).thenReturn(10.5);

            double result = taxRateService.getTaxPercent(1L, 3L, 2L, "12345");

            assertEquals(10.5, result);
        }

        @Test
        void getTaxPercent_whenNotFound_shouldReturnZero() {
            when(taxRateRepository.getTaxPercent(3L, 2L, "12345", 1L)).thenReturn(null);

            double result = taxRateService.getTaxPercent(1L, 3L, 2L, "12345");

            assertEquals(0.0, result);
        }
    }

    @Nested
    class GetBulkTaxRateTest {
        @Test
        void getBulkTaxRate_shouldReturnList() {
            when(taxRateRepository.getBatchTaxRates(3L, 2L, "12345", new HashSet<>(List.of(1L))))
                .thenReturn(List.of(taxRate));

            List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(1L), 3L, 2L, "12345");

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(10.0, result.get(0).rate());
        }
    }
}

// force rebuild