package com.yas.order.service;

import static com.yas.order.utils.SecurityContextUtils.setSubjectUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentMethod;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.model.request.OrderRequest;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.order.OrderItemPostVm;
import com.yas.order.viewmodel.order.OrderListVm;
import com.yas.order.viewmodel.order.OrderPostVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressPostVm;
import com.yas.order.viewmodel.product.ProductVariationVm;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductService productService;
    @Mock private CartService cartService;
    @Mock private OrderMapper orderMapper;
    @Mock private PromotionService promotionService;

    @InjectMocks
    private OrderService orderService;

    private Pair<ZonedDateTime, ZonedDateTime> timePair;
    private Pair<String, String> billingPair;
    private Pair<Integer, Integer> pageInfo;

    @BeforeEach
    void setUp() {
        timePair = Pair.of(ZonedDateTime.now().minusDays(7), ZonedDateTime.now());
        billingPair = Pair.of("Vietnam", "0123");
        pageInfo = Pair.of(0, 10);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static OrderAddressPostVm buildAddressPostVm() {
        return OrderAddressPostVm.builder()
                .contactName("John Doe").phone("0123456789")
                .addressLine1("123 Main St").addressLine2("")
                .city("Hanoi").zipCode("10000")
                .districtId(1L).districtName("Hoan Kiem")
                .stateOrProvinceId(1L).stateOrProvinceName("HN")
                .countryId(1L).countryName("Vietnam")
                .build();
    }

    private static OrderAddress buildAddress() {
        return OrderAddress.builder()
                .contactName("John").phone("0123").addressLine1("123 St")
                .city("HN").zipCode("10000").districtId(1L).districtName("HK")
                .stateOrProvinceId(1L).stateOrProvinceName("HN")
                .countryId(1L).countryName("Vietnam").build();
    }

    private Order buildOrder(long id, OrderStatus status) {
        Order order = Order.builder()
                .email("test@test.com").note("note")
                .tax(0f).discount(0f).numberItem(1)
                .totalPrice(BigDecimal.valueOf(100)).deliveryFee(BigDecimal.ZERO)
                .couponCode("CODE").orderStatus(status)
                .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
                .deliveryStatus(DeliveryStatus.PREPARING)
                .paymentStatus(PaymentStatus.PENDING)
                .shippingAddressId(buildAddress())
                .billingAddressId(buildAddress())
                .checkoutId("checkout-1")
                .build();
        order.setId(id);
        return order;
    }

    private static OrderPostVm buildOrderPostVm() {
        OrderItemPostVm item = OrderItemPostVm.builder()
                .productId(1L).productName("Phone").quantity(1)
                .productPrice(BigDecimal.valueOf(100))
                .note("").build();
        return OrderPostVm.builder()
                .checkoutId("checkout-1").email("test@test.com")
                .shippingAddressPostVm(buildAddressPostVm())
                .billingAddressPostVm(buildAddressPostVm())
                .tax(0f).discount(0f).numberItem(1)
                .totalPrice(BigDecimal.valueOf(100)).deliveryFee(BigDecimal.ZERO)
                .couponCode("CODE")
                .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .orderItemPostVms(List.of(item))
                .build();
    }

    // ── createOrder ───────────────────────────────────────────────────────────

    @Test
    void createOrder_savesOrderAndItems_returnsOrderVm() {
        OrderPostVm vm = buildOrderPostVm();
        Order saved = buildOrder(1L, OrderStatus.PENDING);

        when(orderRepository.save(any())).thenReturn(saved);
        when(orderItemRepository.saveAll(any())).thenReturn(List.of());
        when(orderRepository.findById(any())).thenReturn(Optional.of(saved));
        doNothing().when(cartService).deleteCartItems(any());
        doNothing().when(productService).subtractProductStockQuantity(any());
        doNothing().when(promotionService).updateUsagePromotion(anyList());

        OrderVm result = orderService.createOrder(vm);

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("test@test.com");
        assertThat(result.checkoutId()).isEqualTo("checkout-1");
        verify(orderItemRepository).saveAll(any());
    }

    // ── getOrderWithItemsById ─────────────────────────────────────────────────

    @Test
    void getOrderWithItemsById_whenFound_returnsVmWithItems() {
        Order order = buildOrder(10L, OrderStatus.ACCEPTED);
        OrderItem item = OrderItem.builder()
                .productId(1L).productName("Phone").quantity(1)
                .productPrice(BigDecimal.valueOf(100)).orderId(10L).build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(10L)).thenReturn(List.of(item));

        OrderVm result = orderService.getOrderWithItemsById(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.orderItemVms()).hasSize(1);
    }

    @Test
    void getOrderWithItemsById_whenNotFound_throwsNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.getOrderWithItemsById(99L));
    }

    // ── getLatestOrders ───────────────────────────────────────────────────────

    @Test
    void getLatestOrders_whenCountZero_returnsEmpty() {
        assertThat(orderService.getLatestOrders(0)).isEmpty();
    }

    @Test
    void getLatestOrders_whenCountNegative_returnsEmpty() {
        assertThat(orderService.getLatestOrders(-5)).isEmpty();
    }

    @Test
    void getLatestOrders_whenNoOrders_returnsEmpty() {
        when(orderRepository.getLatestOrders(any())).thenReturn(List.of());
        assertThat(orderService.getLatestOrders(5)).isEmpty();
    }

    @Test
    void getLatestOrders_whenOrdersExist_returnsMappedList() {
        Order order = buildOrder(1L, OrderStatus.COMPLETED);
        when(orderRepository.getLatestOrders(any())).thenReturn(List.of(order));

        List<OrderBriefVm> result = orderService.getLatestOrders(3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    // ── getAllOrder ────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getAllOrder_whenNoResults_returnsEmptyVm() {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        OrderListVm result = orderService.getAllOrder(timePair, "", List.of(),
                billingPair, "test@test.com", pageInfo);

        assertThat(result.orderList()).isNull();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllOrder_whenOrdersExist_returnsList() {
        Order order = buildOrder(1L, OrderStatus.PENDING);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        OrderListVm result = orderService.getAllOrder(timePair, "", List.of(OrderStatus.PENDING),
                billingPair, "", pageInfo);

        assertThat(result.orderList()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllOrder_whenStatusListEmpty_usesAllStatuses() {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        OrderListVm result = orderService.getAllOrder(timePair, null, List.of(), billingPair, null, pageInfo);
        assertThat(result).isNotNull();
    }

    // ── updateOrderPaymentStatus ──────────────────────────────────────────────

    @Test
    void updateOrderPaymentStatus_whenCompleted_setsOrderStatusPaid() {
        Order order = buildOrder(1L, OrderStatus.PENDING);
        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                .orderId(1L).paymentId(42L)
                .paymentStatus(PaymentStatus.COMPLETED.name()).orderStatus("PENDING").build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(vm);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.paymentId()).isEqualTo(42L);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderPaymentStatus_whenNotCompleted_doesNotChangeToPaid() {
        Order order = buildOrder(2L, OrderStatus.PENDING);
        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                .orderId(2L).paymentId(10L)
                .paymentStatus(PaymentStatus.CANCELLED.name()).orderStatus("PENDING").build();

        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.updateOrderPaymentStatus(vm);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void updateOrderPaymentStatus_whenOrderNotFound_throwsNotFoundException() {
        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                .orderId(99L).paymentId(1L)
                .paymentStatus(PaymentStatus.PENDING.name()).orderStatus("PENDING").build();
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.updateOrderPaymentStatus(vm));
    }

    // ── rejectOrder ───────────────────────────────────────────────────────────

    @Test
    void rejectOrder_setsStatusAndReason() {
        Order order = buildOrder(5L, OrderStatus.PENDING);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.rejectOrder(5L, "Out of stock");

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECT);
        assertThat(order.getRejectReason()).isEqualTo("Out of stock");
        verify(orderRepository).save(order);
    }

    @Test
    void rejectOrder_whenNotFound_throwsNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.rejectOrder(99L, "reason"));
    }

    // ── acceptOrder ───────────────────────────────────────────────────────────

    @Test
    void acceptOrder_setsStatusAccepted() {
        Order order = buildOrder(3L, OrderStatus.PENDING);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.acceptOrder(3L);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
        verify(orderRepository).save(order);
    }

    @Test
    void acceptOrder_whenNotFound_throwsNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.acceptOrder(99L));
    }

    // ── findOrderByCheckoutId ─────────────────────────────────────────────────

    @Test
    void findOrderByCheckoutId_whenFound_returnsOrder() {
        Order order = buildOrder(7L, OrderStatus.PENDING);
        when(orderRepository.findByCheckoutId("chk-1")).thenReturn(Optional.of(order));

        Order result = orderService.findOrderByCheckoutId("chk-1");
        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    void findOrderByCheckoutId_whenNotFound_throwsNotFoundException() {
        when(orderRepository.findByCheckoutId("missing")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.findOrderByCheckoutId("missing"));
    }

    // ── findOrderVmByCheckoutId ───────────────────────────────────────────────

    @Test
    void findOrderVmByCheckoutId_whenFound_returnsVmWithItems() {
        Order order = buildOrder(8L, OrderStatus.PAID);
        OrderItem item = OrderItem.builder()
                .productId(1L).productName("P").quantity(2)
                .productPrice(BigDecimal.TEN).orderId(8L).build();

        when(orderRepository.findByCheckoutId("chk-2")).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(8L)).thenReturn(List.of(item));

        var result = orderService.findOrderVmByCheckoutId("chk-2");

        assertThat(result.id()).isEqualTo(8L);
        assertThat(result.orderItems()).hasSize(1);
    }

    @Test
    void findOrderVmByCheckoutId_whenNotFound_throwsNotFoundException() {
        when(orderRepository.findByCheckoutId("gone")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.findOrderVmByCheckoutId("gone"));
    }

    // ── isOrderCompletedWithUserIdAndProductId ────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void isOrderCompleted_whenNoVariations_usesProductIdDirectly() {
        setSubjectUpSecurityContext("user-1");
        when(productService.getProductVariations(1L)).thenReturn(List.of());
        when(orderRepository.findOne(any(Specification.class)))
                .thenReturn(Optional.of(buildOrder(1L, OrderStatus.COMPLETED)));

        var result = orderService.isOrderCompletedWithUserIdAndProductId(1L);
        assertThat(result.isPresent()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void isOrderCompleted_whenVariationsExist_usesVariationIds() {
        setSubjectUpSecurityContext("user-2");
        ProductVariationVm v1 = new ProductVariationVm(10L, "Red", "sku-red");
        ProductVariationVm v2 = new ProductVariationVm(11L, "Blue", "sku-blue");
        when(productService.getProductVariations(5L)).thenReturn(List.of(v1, v2));
        when(orderRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        var result = orderService.isOrderCompletedWithUserIdAndProductId(5L);
        assertThat(result.isPresent()).isFalse();
    }

    // ── getMyOrders ───────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getMyOrders_returnsOrderGetVmList() {
        setSubjectUpSecurityContext("user-3");
        Order order = buildOrder(20L, OrderStatus.SHIPPING);
        when(orderRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(order));

        var result = orderService.getMyOrders("Phone", OrderStatus.SHIPPING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(20L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMyOrders_whenNoOrders_returnsEmptyList() {
        setSubjectUpSecurityContext("user-4");
        when(orderRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of());

        var result = orderService.getMyOrders(null, null);
        assertThat(result).isEmpty();
    }

    // ── exportCsv ─────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void exportCsv_whenNoOrders_returnsEmptyCsvBytes() throws Exception {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        OrderRequest req = OrderRequest.builder()
                .createdFrom(ZonedDateTime.now().minusDays(1)).createdTo(ZonedDateTime.now())
                .productName("").orderStatus(List.of())
                .billingCountry("").billingPhoneNumber("").email("")
                .pageNo(0).pageSize(10).build();

        byte[] csv = orderService.exportCsv(req);
        assertThat(csv).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportCsv_whenOrdersExist_returnsCsvWithRows() throws Exception {
        Order order = buildOrder(1L, OrderStatus.COMPLETED);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        com.yas.order.model.csv.OrderItemCsv csvRow =
                com.yas.order.model.csv.OrderItemCsv.builder().build();
        when(orderMapper.toCsv(any())).thenReturn(csvRow);

        OrderRequest req = OrderRequest.builder()
                .createdFrom(ZonedDateTime.now().minusDays(7)).createdTo(ZonedDateTime.now())
                .productName("").orderStatus(List.of(OrderStatus.COMPLETED))
                .billingCountry("Vietnam").billingPhoneNumber("0123").email("")
                .pageNo(0).pageSize(10).build();

        byte[] csv = orderService.exportCsv(req);

        assertThat(csv).isNotNull();
        verify(orderMapper).toCsv(any());
    }
}
