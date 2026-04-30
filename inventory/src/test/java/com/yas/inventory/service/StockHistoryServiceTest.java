package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.inventory.model.Stock;
import com.yas.inventory.model.StockHistory;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.repository.StockHistoryRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stockhistory.StockHistoryListVm;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockHistoryServiceTest {

    @Mock
    private StockHistoryRepository stockHistoryRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private StockHistoryService stockHistoryService;

    private static final Long PRODUCT_ID  = 100L;
    private static final Long WAREHOUSE_ID = 1L;
    private static final Long STOCK_ID    = 10L;

    private Warehouse sampleWarehouse;
    private Stock sampleStock;

    @BeforeEach
    void setUp() {
        sampleWarehouse = Warehouse.builder()
            .id(WAREHOUSE_ID)
            .name("Test Warehouse")
            .addressId(5L)
            .build();

        sampleStock = Stock.builder()
            .id(STOCK_ID)
            .productId(PRODUCT_ID)
            .warehouse(sampleWarehouse)
            .quantity(100L)
            .reservedQuantity(0L)
            .build();
    }

    // =====================================================================
    // createStockHistories
    // =====================================================================
    @Nested
    class CreateStockHistoriesTest {

        @Test
        void testCreateStockHistories_whenMatchingVmExists_shouldSaveStockHistory() {
            StockQuantityVm vm = new StockQuantityVm(STOCK_ID, 50L, "Restock note");

            when(stockHistoryRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

            stockHistoryService.createStockHistories(List.of(sampleStock), List.of(vm));

            verify(stockHistoryRepository, times(1)).saveAll(anyList());
        }

        @Test
        void testCreateStockHistories_whenMultipleStocksAndVms_shouldSaveAllMatched() {
            Long stock2Id = 20L;
            Stock stock2 = Stock.builder()
                .id(stock2Id)
                .productId(200L)
                .warehouse(sampleWarehouse)
                .quantity(50L)
                .reservedQuantity(0L)
                .build();

            StockQuantityVm vm1 = new StockQuantityVm(STOCK_ID, 10L, "Note 1");
            StockQuantityVm vm2 = new StockQuantityVm(stock2Id, -5L, "Note 2");

            when(stockHistoryRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

            stockHistoryService.createStockHistories(List.of(sampleStock, stock2), List.of(vm1, vm2));

            verify(stockHistoryRepository, times(1)).saveAll(anyList());
        }

        @Test
        void testCreateStockHistories_whenNoMatchingVm_shouldSaveEmptyList() {
            // VM has different stockId — no match → history list should be empty
            StockQuantityVm vm = new StockQuantityVm(999L, 50L, "No match");

            when(stockHistoryRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

            stockHistoryService.createStockHistories(List.of(sampleStock), List.of(vm));

            verify(stockHistoryRepository, times(1)).saveAll(anyList());
        }

        @Test
        void testCreateStockHistories_whenEmptyStockList_shouldSaveEmptyList() {
            StockQuantityVm vm = new StockQuantityVm(STOCK_ID, 50L, "Note");

            when(stockHistoryRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

            stockHistoryService.createStockHistories(Collections.emptyList(), List.of(vm));

            verify(stockHistoryRepository, times(1)).saveAll(anyList());
        }

        @Test
        void testCreateStockHistories_whenBothListsEmpty_shouldSaveEmptyList() {
            when(stockHistoryRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

            stockHistoryService.createStockHistories(Collections.emptyList(), Collections.emptyList());

            verify(stockHistoryRepository, times(1)).saveAll(anyList());
        }

        @Test
        void testCreateStockHistories_whenNoteIsNull_shouldStillSaveHistory() {
            StockQuantityVm vm = new StockQuantityVm(STOCK_ID, 20L, null);

            when(stockHistoryRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

            stockHistoryService.createStockHistories(List.of(sampleStock), List.of(vm));

            verify(stockHistoryRepository, times(1)).saveAll(anyList());
        }
    }

    // =====================================================================
    // getStockHistories
    // =====================================================================
    @Nested
    class GetStockHistoriesTest {

        @Test
        void testGetStockHistories_whenHistoriesExist_shouldReturnListVm() {
            StockHistory history1 = StockHistory.builder()
                .id(1L)
                .productId(PRODUCT_ID)
                .adjustedQuantity(50L)
                .note("Initial stock")
                .warehouse(sampleWarehouse)
                .build();
            StockHistory history2 = StockHistory.builder()
                .id(2L)
                .productId(PRODUCT_ID)
                .adjustedQuantity(-10L)
                .note("Adjustment")
                .warehouse(sampleWarehouse)
                .build();

            ProductInfoVm productInfoVm = new ProductInfoVm(PRODUCT_ID, "Product A", "SKU-A", true);

            when(stockHistoryRepository.findByProductIdAndWarehouseIdOrderByCreatedOnDesc(
                PRODUCT_ID, WAREHOUSE_ID))
                .thenReturn(List.of(history1, history2));
            when(productService.getProduct(PRODUCT_ID)).thenReturn(productInfoVm);

            StockHistoryListVm result = stockHistoryService.getStockHistories(PRODUCT_ID, WAREHOUSE_ID);

            assertNotNull(result);
            assertEquals(2, result.data().size());
            assertEquals("Product A", result.data().get(0).productName());
            assertEquals(50L, result.data().get(0).adjustedQuantity());
            assertEquals("Initial stock", result.data().get(0).note());
            assertEquals(-10L, result.data().get(1).adjustedQuantity());
        }

        @Test
        void testGetStockHistories_whenNoHistoriesExist_shouldReturnEmptyList() {
            ProductInfoVm productInfoVm = new ProductInfoVm(PRODUCT_ID, "Product A", "SKU-A", true);

            when(stockHistoryRepository.findByProductIdAndWarehouseIdOrderByCreatedOnDesc(
                PRODUCT_ID, WAREHOUSE_ID))
                .thenReturn(Collections.emptyList());
            when(productService.getProduct(PRODUCT_ID)).thenReturn(productInfoVm);

            StockHistoryListVm result = stockHistoryService.getStockHistories(PRODUCT_ID, WAREHOUSE_ID);

            assertNotNull(result);
            assertEquals(0, result.data().size());
        }

        @Test
        void testGetStockHistories_whenProductInfoVmReturned_shouldMapProductNameCorrectly() {
            StockHistory history = StockHistory.builder()
                .id(3L)
                .productId(PRODUCT_ID)
                .adjustedQuantity(100L)
                .note("Big restock")
                .warehouse(sampleWarehouse)
                .build();

            ProductInfoVm productInfoVm = new ProductInfoVm(PRODUCT_ID, "Special Product", "SP-001", true);

            when(stockHistoryRepository.findByProductIdAndWarehouseIdOrderByCreatedOnDesc(
                PRODUCT_ID, WAREHOUSE_ID))
                .thenReturn(List.of(history));
            when(productService.getProduct(PRODUCT_ID)).thenReturn(productInfoVm);

            StockHistoryListVm result = stockHistoryService.getStockHistories(PRODUCT_ID, WAREHOUSE_ID);

            assertNotNull(result);
            assertEquals(1, result.data().size());
            assertEquals("Special Product", result.data().get(0).productName());
            assertEquals(3L, result.data().get(0).id());
        }

        @Test
        void testGetStockHistories_verifyRepositoryCalledWithCorrectArgs() {
            ProductInfoVm productInfoVm = new ProductInfoVm(PRODUCT_ID, "Product", "SKU", true);

            when(stockHistoryRepository.findByProductIdAndWarehouseIdOrderByCreatedOnDesc(
                PRODUCT_ID, WAREHOUSE_ID))
                .thenReturn(Collections.emptyList());
            when(productService.getProduct(PRODUCT_ID)).thenReturn(productInfoVm);

            stockHistoryService.getStockHistories(PRODUCT_ID, WAREHOUSE_ID);

            verify(stockHistoryRepository, times(1))
                .findByProductIdAndWarehouseIdOrderByCreatedOnDesc(PRODUCT_ID, WAREHOUSE_ID);
            verify(productService, times(1)).getProduct(PRODUCT_ID);
        }
    }
}
