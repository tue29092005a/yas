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

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
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
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TaxClassServiceTest {

    @Mock
    private TaxClassRepository taxClassRepository;

    @InjectMocks
    private TaxClassService taxClassService;

    private TaxClass taxClass;

    @BeforeEach
    void setUp() {
        taxClass = new TaxClass();
        taxClass.setId(1L);
        taxClass.setName("Standard");
    }

    @Nested
    class FindAllTaxClassesTest {
        @Test
        void findAllTaxClasses_shouldReturnList() {
            when(taxClassRepository.findAll(any(Sort.class))).thenReturn(List.of(taxClass));

            List<TaxClassVm> result = taxClassService.findAllTaxClasses();

            assertFalse(result.isEmpty());
            assertEquals(1L, result.get(0).id());
            assertEquals("Standard", result.get(0).name());
        }
    }

    @Nested
    class FindByIdTest {
        @Test
        void findById_whenTaxClassExists_shouldReturnTaxClassVm() {
            when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));

            TaxClassVm result = taxClassService.findById(1L);

            assertNotNull(result);
            assertEquals(1L, result.id());
            assertEquals("Standard", result.name());
        }

        @Test
        void findById_whenTaxClassDoesNotExist_shouldThrowNotFoundException() {
            when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> taxClassService.findById(1L));
        }
    }

    @Nested
    class CreateTest {
        @Test
        void create_whenNameAlreadyExists_shouldThrowDuplicatedException() {
            TaxClassPostVm postVm = new TaxClassPostVm(null, "Standard");
            when(taxClassRepository.existsByName("Standard")).thenReturn(true);

            assertThrows(DuplicatedException.class, () -> taxClassService.create(postVm));
            verify(taxClassRepository, never()).save(any());
        }

        @Test
        void create_whenValidRequest_shouldSaveAndReturnTaxClass() {
            TaxClassPostVm postVm = new TaxClassPostVm(null, "Standard");
            when(taxClassRepository.existsByName("Standard")).thenReturn(false);
            when(taxClassRepository.save(any(TaxClass.class))).thenReturn(taxClass);

            TaxClass result = taxClassService.create(postVm);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Standard", result.getName());
            verify(taxClassRepository, times(1)).save(any(TaxClass.class));
        }
    }

    @Nested
    class UpdateTest {
        @Test
        void update_whenTaxClassDoesNotExist_shouldThrowNotFoundException() {
            TaxClassPostVm postVm = new TaxClassPostVm("1", "Updated");
            when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> taxClassService.update(postVm, 1L));
            verify(taxClassRepository, never()).save(any());
        }

        @Test
        void update_whenNameAlreadyExistsForOtherTaxClass_shouldThrowDuplicatedException() {
            TaxClassPostVm postVm = new TaxClassPostVm("1", "Updated");
            when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
            when(taxClassRepository.existsByNameNotUpdatingTaxClass("Updated", 1L)).thenReturn(true);

            assertThrows(DuplicatedException.class, () -> taxClassService.update(postVm, 1L));
            verify(taxClassRepository, never()).save(any());
        }

        @Test
        void update_whenValidRequest_shouldUpdateAndSaveTaxClass() {
            TaxClassPostVm postVm = new TaxClassPostVm("1", "Updated");
            when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
            when(taxClassRepository.existsByNameNotUpdatingTaxClass("Updated", 1L)).thenReturn(false);
            when(taxClassRepository.save(any(TaxClass.class))).thenReturn(taxClass);

            taxClassService.update(postVm, 1L);

            assertEquals("Updated", taxClass.getName());
            verify(taxClassRepository, times(1)).save(taxClass);
        }
    }

    @Nested
    class DeleteTest {
        @Test
        void delete_whenTaxClassDoesNotExist_shouldThrowNotFoundException() {
            when(taxClassRepository.existsById(1L)).thenReturn(false);

            assertThrows(NotFoundException.class, () -> taxClassService.delete(1L));
            verify(taxClassRepository, never()).deleteById(any());
        }

        @Test
        void delete_whenTaxClassExists_shouldDelete() {
            when(taxClassRepository.existsById(1L)).thenReturn(true);
            doNothing().when(taxClassRepository).deleteById(1L);

            taxClassService.delete(1L);

            verify(taxClassRepository, times(1)).deleteById(1L);
        }
    }

    @Nested
    class GetPageableTaxClassesTest {
        @Test
        void getPageableTaxClasses_shouldReturnTaxClassListGetVm() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<TaxClass> page = new PageImpl<>(List.of(taxClass), pageable, 1);
            when(taxClassRepository.findAll(any(Pageable.class))).thenReturn(page);

            TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);

            assertNotNull(result);
            assertEquals(1, result.taxClassContent().size());
            assertEquals(1L, result.taxClassContent().get(0).id());
            assertEquals("Standard", result.taxClassContent().get(0).name());
            assertEquals(0, result.pageNo());
            assertEquals(10, result.pageSize());
            assertEquals(1, result.totalElements());
            assertEquals(1, result.totalPages());
            assertTrue(result.isLast());
        }
    }
}
