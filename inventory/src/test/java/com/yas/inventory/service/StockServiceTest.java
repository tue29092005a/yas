package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
// test
import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.StockExistingException;
import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.stock.StockPostVm;
import com.yas.inventory.viewmodel.stock.StockQuantityUpdateVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stock.StockVm;
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

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private StockHistoryService stockHistoryService;

    @InjectMocks
    private StockService stockService;

    private static final Long WAREHOUSE_ID = 1L;
    private static final Long PRODUCT_ID = 100L;
    private static final Long STOCK_ID = 10L;

    private Warehouse sampleWarehouse;

    @BeforeEach
    void setUp() {
        sampleWarehouse = Warehouse.builder()
            .id(WAREHOUSE_ID)
            .name("Test Warehouse")
            .addressId(5L)
            .build();
    }

    // =====================================================================
    // addProductIntoWarehouse
    // =====================================================================
    @Nested
    class AddProductIntoWarehouseTest {

        @Test
        void testAddProductIntoWarehouse_whenStockAlreadyExists_shouldThrowStockExistingException() {
            StockPostVm postVm = new StockPostVm(PRODUCT_ID, WAREHOUSE_ID);

            when(stockRepository.existsByWarehouseIdAndProductId(WAREHOUSE_ID, PRODUCT_ID)).thenReturn(true);

            assertThrows(StockExistingException.class,
                () -> stockService.addProductIntoWarehouse(List.of(postVm)));

            verify(stockRepository, never()).saveAll(anyList());
        }

        @Test
        void testAddProductIntoWarehouse_whenProductNotFound_shouldThrowNotFoundException() {
            StockPostVm postVm = new StockPostVm(PRODUCT_ID, WAREHOUSE_ID);

            when(stockRepository.existsByWarehouseIdAndProductId(WAREHOUSE_ID, PRODUCT_ID)).thenReturn(false);
            when(productService.getProduct(PRODUCT_ID)).thenReturn(null);

            assertThrows(NotFoundException.class,
                () -> stockService.addProductIntoWarehouse(List.of(postVm)));

            verify(stockRepository, never()).saveAll(anyList());
        }

        @Test
        void testAddProductIntoWarehouse_whenWarehouseNotFound_shouldThrowNotFoundException() {
            StockPostVm postVm = new StockPostVm(PRODUCT_ID, WAREHOUSE_ID);
            ProductInfoVm productInfoVm = new ProductInfoVm(PRODUCT_ID, "Product A", "SKU-A", false);

            when(stockRepository.existsByWarehouseIdAndProductId(WAREHOUSE_ID, PRODUCT_ID)).thenReturn(false);
            when(productService.getProduct(PRODUCT_ID)).thenReturn(productInfoVm);
            when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                () -> stockService.addProductIntoWarehouse(List.of(postVm)));

            verify(stockRepository, never()).saveAll(anyList());
        }

        @Test
        void testAddProductIntoWarehouse_whenValidInput_shouldSaveStock() {
            StockPostVm postVm = new StockPostVm(PRODUCT_ID, WAREHOUSE_ID);
            ProductInfoVm productInfoVm = new ProductInfoVm(PRODUCT_ID, "Product A", "SKU-A", false);

            when(stockRepository.existsByWarehouseIdAndProductId(WAREHOUSE_ID, PRODUCT_ID)).thenReturn(false);
            when(productService.getProduct(PRODUCT_ID)).thenReturn(productInfoVm);
            when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(sampleWarehouse));
            when(stockRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> stockService.addProductIntoWarehouse(List.of(postVm)));

            verify(stockRepository, times(1)).saveAll(anyList());
        }

        @Test
        void testAddProductIntoWarehouse_whenEmptyList_shouldSaveEmptyList() {
            assertDoesNotThrow(() -> stockService.addProductIntoWarehouse(Collections.emptyList()));
            verify(stockRepository, times(1)).saveAll(anyList());
        }

        @Test
        void testAddProductIntoWarehouse_whenMultipleValidItems_shouldSaveAll() {
            Long product2 = 200L;
            StockPostVm postVm1 = new StockPostVm(PRODUCT_ID, WAREHOUSE_ID);
            StockPostVm postVm2 = new StockPostVm(product2, WAREHOUSE_ID);

            ProductInfoVm productInfoVm1 = new ProductInfoVm(PRODUCT_ID, "Product A", "SKU-A", false);
            ProductInfoVm productInfoVm2 = new ProductInfoVm(product2, "Product B", "SKU-B", false);

            when(stockRepository.existsByWarehouseIdAndProductId(WAREHOUSE_ID, PRODUCT_ID)).thenReturn(false);
            when(stockRepository.existsByWarehouseIdAndProductId(WAREHOUSE_ID, product2)).thenReturn(false);
            when(productService.getProduct(PRODUCT_ID)).thenReturn(productInfoVm1);
            when(productService.getProduct(product2)).thenReturn(productInfoVm2);
            when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(sampleWarehouse));
            when(stockRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> stockService.addProductIntoWarehouse(List.of(postVm1, postVm2)));

            verify(stockRepository, times(1)).saveAll(anyList());
        }
    }

    // =====================================================================
    // getStocksByWarehouseIdAndProductNameAndSku
    // =====================================================================
    @Nested
    class GetStocksByWarehouseIdAndProductNameAndSkuTest {

        @Test
        void testGetStocks_whenValidInput_shouldReturnStockVmList() {
            ProductInfoVm productInfoVm = new ProductInfoVm(PRODUCT_ID, "Product A", "SKU-A", true);
            Stock stock = Stock.builder()
                .id(STOCK_ID)
                .productId(PRODUCT_ID)
                .warehouse(sampleWarehouse)
                .quantity(50L)
                .reservedQuantity(5L)
                .build();

            when(warehouseService.getProductWarehouse(
                anyLong(), any(), any(), any(FilterExistInWhSelection.class)))
                .thenReturn(List.of(productInfoVm));
            when(stockRepository.findByWarehouseIdAndProductIdIn(anyLong(), anyList()))
                .thenReturn(List.of(stock));

            List<StockVm> result = stockService.getStocksByWarehouseIdAndProductNameAndSku(
                WAREHOUSE_ID, "Product A", "SKU-A");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(STOCK_ID, result.get(0).id());
            assertEquals(PRODUCT_ID, result.get(0).productId());
            assertEquals("Product A", result.get(0).productName());
            assertEquals("SKU-A", result.get(0).productSku());
            assertEquals(50L, result.get(0).quantity());
        }

        @Test
        void testGetStocks_whenNoProductsInWarehouse_shouldReturnEmptyList() {
            when(warehouseService.getProductWarehouse(
                anyLong(), any(), any(), any(FilterExistInWhSelection.class)))
                .thenReturn(Collections.emptyList());
            when(stockRepository.findByWarehouseIdAndProductIdIn(anyLong(), anyList()))
                .thenReturn(Collections.emptyList());

            List<StockVm> result = stockService.getStocksByWarehouseIdAndProductNameAndSku(
                WAREHOUSE_ID, null, null);

            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    // =====================================================================
    // updateProductQuantityInStock
    // =====================================================================
    @Nested
    class UpdateProductQuantityInStockTest {

        @Test
        void testUpdateProductQuantity_whenValidPositiveAdjustment_shouldUpdateAndSave() {
            Stock stock = Stock.builder()
                .id(STOCK_ID)
                .productId(PRODUCT_ID)
                .warehouse(sampleWarehouse)
                .quantity(100L)
                .reservedQuantity(0L)
                .build();

            StockQuantityVm stockQuantityVm = new StockQuantityVm(STOCK_ID, 50L, "Restock");
            StockQuantityUpdateVm requestBody = new StockQuantityUpdateVm(List.of(stockQuantityVm));

            when(stockRepository.findAllById(anyList())).thenReturn(List.of(stock));
            when(stockRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> stockService.updateProductQuantityInStock(requestBody));

            assertEquals(150L, stock.getQuantity());
            verify(stockRepository, times(1)).saveAll(anyList());
            verify(stockHistoryService, times(1)).createStockHistories(any(), any());
            verify(productService, times(1)).updateProductQuantity(anyList());
        }

        @Test
        void testUpdateProductQuantity_whenValidNegativeAdjustment_shouldDecreaseQuantity() {
            Stock stock = Stock.builder()
                .id(STOCK_ID)
                .productId(PRODUCT_ID)
                .warehouse(sampleWarehouse)
                .quantity(100L)
                .reservedQuantity(0L)
                .build();

            // quantity is negative but NOT (negative AND greater than current): -10 < 0 but -10 < 100, so condition false
            // Actually condition is: adjustedQty < 0 && adjustedQty > stock.getQuantity()
            // -10 < 0 is true, but -10 > 100 is false => no exception
            StockQuantityVm stockQuantityVm = new StockQuantityVm(STOCK_ID, -10L, "Return");
            StockQuantityUpdateVm requestBody = new StockQuantityUpdateVm(List.of(stockQuantityVm));

            when(stockRepository.findAllById(anyList())).thenReturn(List.of(stock));
            when(stockRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> stockService.updateProductQuantityInStock(requestBody));

            assertEquals(90L, stock.getQuantity());
        }

        @Test
        void testUpdateProductQuantity_whenAdjustedQuantityIsNull_shouldTreatAsZero() {
            Stock stock = Stock.builder()
                .id(STOCK_ID)
                .productId(PRODUCT_ID)
                .warehouse(sampleWarehouse)
                .quantity(100L)
                .reservedQuantity(0L)
                .build();

            StockQuantityVm stockQuantityVm = new StockQuantityVm(STOCK_ID, null, "No change");
            StockQuantityUpdateVm requestBody = new StockQuantityUpdateVm(List.of(stockQuantityVm));

            when(stockRepository.findAllById(anyList())).thenReturn(List.of(stock));
            when(stockRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> stockService.updateProductQuantityInStock(requestBody));

            // null => 0, so quantity remains 100
            assertEquals(100L, stock.getQuantity());
        }

        @Test
        void testUpdateProductQuantity_whenInvalidNegativeQuantity_shouldThrowBadRequestException() {
            // adjustedQty < 0 AND adjustedQty > stock.getQuantity() => exception
            // e.g., current = -5 (already negative), adjustment = -10, -10 > -5 is false, no exception
            // To trigger: stock.quantity = -5, adjustment = -4 => -4 < 0 && -4 > -5 => true
            Stock stock = Stock.builder()
                .id(STOCK_ID)
                .productId(PRODUCT_ID)
                .warehouse(sampleWarehouse)
                .quantity(-5L)
                .reservedQuantity(0L)
                .build();

            StockQuantityVm stockQuantityVm = new StockQuantityVm(STOCK_ID, -4L, "Invalid");
            StockQuantityUpdateVm requestBody = new StockQuantityUpdateVm(List.of(stockQuantityVm));

            when(stockRepository.findAllById(anyList())).thenReturn(List.of(stock));

            assertThrows(BadRequestException.class,
                () -> stockService.updateProductQuantityInStock(requestBody));

            verify(stockRepository, never()).saveAll(anyList());
        }

        @Test
        void testUpdateProductQuantity_whenStockIdNotMatchingAnyVm_shouldSkipStock() {
            Stock stock = Stock.builder()
                .id(STOCK_ID)
                .productId(PRODUCT_ID)
                .warehouse(sampleWarehouse)
                .quantity(100L)
                .reservedQuantity(0L)
                .build();

            // VM has different stockId
            StockQuantityVm stockQuantityVm = new StockQuantityVm(999L, 50L, "No match");
            StockQuantityUpdateVm requestBody = new StockQuantityUpdateVm(List.of(stockQuantityVm));

            when(stockRepository.findAllById(anyList())).thenReturn(List.of(stock));
            when(stockRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> stockService.updateProductQuantityInStock(requestBody));

            // quantity should remain unchanged since no matching vm was found
            assertEquals(100L, stock.getQuantity());
        }

        @Test
        void testUpdateProductQuantity_whenStockListEmpty_shouldNotCallProductService() {
            StockQuantityVm stockQuantityVm = new StockQuantityVm(STOCK_ID, 50L, "Note");
            StockQuantityUpdateVm requestBody = new StockQuantityUpdateVm(List.of(stockQuantityVm));

            when(stockRepository.findAllById(anyList())).thenReturn(Collections.emptyList());
            when(stockRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> stockService.updateProductQuantityInStock(requestBody));

            verify(productService, never()).updateProductQuantity(anyList());
        }
    }
}
