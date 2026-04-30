package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.address.AddressDetailVm;
import com.yas.inventory.viewmodel.address.AddressPostVm;
import com.yas.inventory.viewmodel.address.AddressVm;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseDetailVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseGetVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseListGetVm;
import com.yas.inventory.viewmodel.warehouse.WarehousePostVm;
import java.util.Collections;
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
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductService productService;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private WarehouseService warehouseService;

    private static final Long WAREHOUSE_ID = 1L;
    private static final Long ADDRESS_ID = 10L;

    private Warehouse sampleWarehouse;
    private WarehousePostVm samplePostVm;
    private AddressDetailVm sampleAddressDetailVm;

    @BeforeEach
    void setUp() {
        sampleWarehouse = Warehouse.builder()
            .id(WAREHOUSE_ID)
            .name("Main Warehouse")
            .addressId(ADDRESS_ID)
            .build();

        samplePostVm = WarehousePostVm.builder()
            .name("Main Warehouse")
            .contactName("John Doe")
            .phone("0123456789")
            .addressLine1("123 Main St")
            .addressLine2("Apt 4")
            .city("Ho Chi Minh")
            .zipCode("70000")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();

        sampleAddressDetailVm = AddressDetailVm.builder()
            .id(ADDRESS_ID)
            .contactName("John Doe")
            .phone("0123456789")
            .addressLine1("123 Main St")
            .addressLine2("Apt 4")
            .city("Ho Chi Minh")
            .zipCode("70000")
            .districtId(1L)
            .districtName("District 1")
            .stateOrProvinceId(2L)
            .stateOrProvinceName("HCM Province")
            .countryId(3L)
            .countryName("Vietnam")
            .build();
    }

    // =====================================================================
    // findAllWarehouses
    // =====================================================================
    @Nested
    class FindAllWarehousesTest {

        @Test
        void testFindAllWarehouses_whenWarehousesExist_shouldReturnList() {
            when(warehouseRepository.findAll()).thenReturn(List.of(sampleWarehouse));

            List<WarehouseGetVm> result = warehouseService.findAllWarehouses();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(WAREHOUSE_ID, result.get(0).id());
            assertEquals("Main Warehouse", result.get(0).name());
        }

        @Test
        void testFindAllWarehouses_whenNoWarehouses_shouldReturnEmptyList() {
            when(warehouseRepository.findAll()).thenReturn(Collections.emptyList());

            List<WarehouseGetVm> result = warehouseService.findAllWarehouses();

            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    // =====================================================================
    // getProductWarehouse
    // =====================================================================
    @Nested
    class GetProductWarehouseTest {

        @Test
        void testGetProductWarehouse_whenProductIdsNotEmpty_shouldReturnMappedList() {
            List<Long> productIdsInWh = List.of(100L, 200L);
            ProductInfoVm vm1 = new ProductInfoVm(100L, "Product A", "SKU-A", false);
            ProductInfoVm vm2 = new ProductInfoVm(200L, "Product B", "SKU-B", false);

            when(stockRepository.getProductIdsInWarehouse(WAREHOUSE_ID)).thenReturn(productIdsInWh);
            when(productService.filterProducts(anyString(), anyString(), anyList(),
                any(FilterExistInWhSelection.class)))
                .thenReturn(List.of(vm1, vm2));

            List<ProductInfoVm> result = warehouseService.getProductWarehouse(
                WAREHOUSE_ID, "Product", "SKU", FilterExistInWhSelection.YES);

            assertNotNull(result);
            assertEquals(2, result.size());
            // existInWh should be true since both ids are in productIdsInWh
            assertEquals(true, result.get(0).existInWh());
            assertEquals(true, result.get(1).existInWh());
        }

        @Test
        void testGetProductWarehouse_whenProductIdsEmpty_shouldReturnRawList() {
            ProductInfoVm vm = new ProductInfoVm(100L, "Product A", "SKU-A", false);

            when(stockRepository.getProductIdsInWarehouse(WAREHOUSE_ID)).thenReturn(Collections.emptyList());
            when(productService.filterProducts(any(), any(), anyList(),
                any(FilterExistInWhSelection.class)))
                .thenReturn(List.of(vm));

            List<ProductInfoVm> result = warehouseService.getProductWarehouse(
                WAREHOUSE_ID, null, null, FilterExistInWhSelection.ALL);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(100L, result.get(0).id());
        }

        @Test
        void testGetProductWarehouse_whenProductNotInWarehouse_existInWhShouldBeFalse() {
            List<Long> productIdsInWh = List.of(100L);
            // Product 200 is returned by filter but NOT in warehouse
            ProductInfoVm vm = new ProductInfoVm(200L, "Product B", "SKU-B", false);

            when(stockRepository.getProductIdsInWarehouse(WAREHOUSE_ID)).thenReturn(productIdsInWh);
            when(productService.filterProducts(any(), any(), anyList(),
                any(FilterExistInWhSelection.class)))
                .thenReturn(List.of(vm));

            List<ProductInfoVm> result = warehouseService.getProductWarehouse(
                WAREHOUSE_ID, null, null, FilterExistInWhSelection.YES);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(false, result.get(0).existInWh());
        }
    }

    // =====================================================================
    // findById
    // =====================================================================
    @Nested
    class FindByIdTest {

        @Test
        void testFindById_whenWarehouseExists_shouldReturnDetailVm() {
            when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(sampleWarehouse));
            when(locationService.getAddressById(ADDRESS_ID)).thenReturn(sampleAddressDetailVm);

            WarehouseDetailVm result = warehouseService.findById(WAREHOUSE_ID);

            assertNotNull(result);
            assertEquals(WAREHOUSE_ID, result.id());
            assertEquals("Main Warehouse", result.name());
            assertEquals("John Doe", result.contactName());
            assertEquals("0123456789", result.phone());
            assertEquals("Ho Chi Minh", result.city());
        }

        @Test
        void testFindById_whenWarehouseNotFound_shouldThrowNotFoundException() {
            when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                () -> warehouseService.findById(WAREHOUSE_ID));

            verify(locationService, never()).getAddressById(anyLong());
        }
    }

    // =====================================================================
    // create
    // =====================================================================
    @Nested
    class CreateWarehouseTest {

        @Test
        void testCreate_whenNameAlreadyExists_shouldThrowDuplicatedException() {
            when(warehouseRepository.existsByName(samplePostVm.name())).thenReturn(true);

            assertThrows(DuplicatedException.class,
                () -> warehouseService.create(samplePostVm));

            verify(locationService, never()).createAddress(any());
            verify(warehouseRepository, never()).save(any());
        }

        @Test
        void testCreate_whenValidInput_shouldSaveAndReturnWarehouse() {
            AddressVm addressVm = AddressVm.builder().id(ADDRESS_ID).build();

            when(warehouseRepository.existsByName(samplePostVm.name())).thenReturn(false);
            when(locationService.createAddress(any(AddressPostVm.class))).thenReturn(addressVm);
            when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> {
                Warehouse wh = inv.getArgument(0);
                wh.setId(WAREHOUSE_ID);
                return wh;
            });

            Warehouse result = warehouseService.create(samplePostVm);

            assertNotNull(result);
            assertEquals("Main Warehouse", result.getName());
            assertEquals(ADDRESS_ID, result.getAddressId());
            verify(warehouseRepository, times(1)).save(any(Warehouse.class));
        }
    }

    // =====================================================================
    // update
    // =====================================================================
    @Nested
    class UpdateWarehouseTest {

        @Test
        void testUpdate_whenWarehouseNotFound_shouldThrowNotFoundException() {
            when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                () -> warehouseService.update(samplePostVm, WAREHOUSE_ID));

            verify(warehouseRepository, never()).save(any());
        }

        @Test
        void testUpdate_whenNameUsedByDifferentWarehouse_shouldThrowDuplicatedException() {
            when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(sampleWarehouse));
            when(warehouseRepository.existsByNameWithDifferentId(samplePostVm.name(), WAREHOUSE_ID))
                .thenReturn(true);

            assertThrows(DuplicatedException.class,
                () -> warehouseService.update(samplePostVm, WAREHOUSE_ID));

            verify(warehouseRepository, never()).save(any());
        }

        @Test
        void testUpdate_whenValidInput_shouldUpdateWarehouse() {
            when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(sampleWarehouse));
            when(warehouseRepository.existsByNameWithDifferentId(samplePostVm.name(), WAREHOUSE_ID))
                .thenReturn(false);
            when(warehouseRepository.save(any(Warehouse.class))).thenReturn(sampleWarehouse);

            assertDoesNotThrow(() -> warehouseService.update(samplePostVm, WAREHOUSE_ID));

            assertEquals("Main Warehouse", sampleWarehouse.getName());
            verify(locationService, times(1)).updateAddress(anyLong(), any(AddressPostVm.class));
            verify(warehouseRepository, times(1)).save(sampleWarehouse);
        }
    }

    // =====================================================================
    // delete
    // =====================================================================
    @Nested
    class DeleteWarehouseTest {

        @Test
        void testDelete_whenWarehouseNotFound_shouldThrowNotFoundException() {
            when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                () -> warehouseService.delete(WAREHOUSE_ID));

            verify(warehouseRepository, never()).deleteById(anyLong());
        }

        @Test
        void testDelete_whenWarehouseExists_shouldDeleteAndRemoveAddress() {
            when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(sampleWarehouse));

            assertDoesNotThrow(() -> warehouseService.delete(WAREHOUSE_ID));

            verify(warehouseRepository, times(1)).deleteById(WAREHOUSE_ID);
            verify(locationService, times(1)).deleteAddress(ADDRESS_ID);
        }
    }

    // =====================================================================
    // getPageableWarehouses
    // =====================================================================
    @Nested
    class GetPageableWarehousesTest {

        @Test
        void testGetPageableWarehouses_whenValidInput_shouldReturnPagedResult() {
            Warehouse w1 = Warehouse.builder().id(1L).name("WH 1").build();
            Warehouse w2 = Warehouse.builder().id(2L).name("WH 2").build();
            List<Warehouse> warehouseList = List.of(w1, w2);
            Page<Warehouse> page = new PageImpl<>(warehouseList);

            when(warehouseRepository.findAll(any(Pageable.class))).thenReturn(page);

            WarehouseListGetVm result = warehouseService.getPageableWarehouses(0, 10);

            assertNotNull(result);
            assertEquals(2, result.warehouseContent().size());
            assertEquals(0, result.pageNo());
            assertEquals(2, result.totalElements());
            assertEquals(1, result.totalPages());
            assertEquals(true, result.isLast());
        }

        @Test
        void testGetPageableWarehouses_whenEmpty_shouldReturnEmptyPage() {
            Page<Warehouse> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                org.springframework.data.domain.PageRequest.of(0, 5),
                0
            );
            when(warehouseRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            WarehouseListGetVm result = warehouseService.getPageableWarehouses(0, 5);

            assertNotNull(result);
            assertEquals(0, result.warehouseContent().size());
            assertEquals(0, result.totalElements());
            assertEquals(0, result.totalPages());
        }
    }
}
