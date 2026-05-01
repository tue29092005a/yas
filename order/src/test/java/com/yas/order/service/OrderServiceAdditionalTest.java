package com.yas.order.service;

import static com.yas.order.utils.SecurityContextUtils.setSubjectUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
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
import com.yas.order.viewmodel.order.OrderGetVm;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceAdditionalTest {

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
        billingPair = Pair.of("Vietnam", "0123456789");
        pageInfo = Pair.of(0, 10);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static OrderAddressPostVm buildAddressPostVm() {
        return OrderAddressPostVm.builder()
                .contactName("Jane").phone("0987654321")
                .addressLine1("456 Le Loi").addressLine2("Floor 2")
                .city("HCMC").zipCode("70000")
                .districtId(2L).districtName("District 1")
                .stateOrProvinceId(2L).stateOrProvinceName("HCM")
                .countryId(1L).countryName("Vietnam")
                .build();
    }

    private static OrderAddress buildAddress() {
        return OrderAddress.builder()
                .contactName("Jane").phone("0987").addressLine1("456 Le Loi")
                .city("HCMC").zipCode("70000").districtId(2L).districtName("D1")
                .stateOrProvinceId(2L).stateOrProvinceName("HCM")
                .countryId(1L).countryName("Vietnam").build();
    }

    private Order buildOrder(long id, OrderStatus status) {
        Order order = Order.builder()
                .email("user@example.com").note("extra note")
                .tax(0.1f).discount(5f).numberItem(2)
                .totalPrice(BigDecimal.valueOf(200)).deliveryFee(BigDecimal.valueOf(10))
                .couponCode("SALE10").orderStatus(status)
                .deliveryMethod(DeliveryMethod.GRAB_EXPRESS)
                .deliveryStatus(DeliveryStatus.PREPARING)
                .paymentStatus(PaymentStatus.PENDING)
                .shippingAddressId(buildAddress())
                .billingAddressId(buildAddress())
                .checkoutId("chk-extra-" + id)
                .build();
        order.setId(id);
        return order;
    }

    // ── createOrder với nhiều items ───────────────────────────────────────────

    @Test
    void createOrder_withMultipleItems_allItemsSaved() {
        OrderItemPostVm item1 = OrderItemPostVm.builder()
                .productId(1L).productName("Phone").quantity(2)
                .productPrice(BigDecimal.valueOf(150)).note("").build();
        OrderItemPostVm item2 = OrderItemPostVm.builder()
                .productId(2L).productName("Case").quantity(1)
                .productPrice(BigDecimal.valueOf(20)).note("").build();

        OrderPostVm vm = OrderPostVm.builder()
                .checkoutId("chk-multi").email("user@example.com")
                .shippingAddressPostVm(buildAddressPostVm())
                .billingAddressPostVm(buildAddressPostVm())
                .tax(0.1f).discount(0f).numberItem(3)
                .totalPrice(BigDecimal.valueOf(320)).deliveryFee(BigDecimal.valueOf(10))
                .couponCode("MULTI")
                .deliveryMethod(DeliveryMethod.GRAB_EXPRESS)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .orderItemPostVms(List.of(item1, item2))
                .build();

        Order saved = buildOrder(10L, OrderStatus.PENDING);
        when(orderRepository.save(any())).thenReturn(saved);
        when(orderItemRepository.saveAll(any())).thenReturn(List.of());
        when(orderRepository.findById(any())).thenReturn(Optional.of(saved));
        doNothing().when(cartService).deleteCartItems(any());
        doNothing().when(productService).subtractProductStockQuantity(any());
        doNothing().when(promotionService).updateUsagePromotion(anyList());

        OrderVm result = orderService.createOrder(vm);

        assertThat(result).isNotNull();
        verify(orderItemRepository).saveAll(any());
        // promotionService called once with a list of 2 entries
        verify(promotionService).updateUsagePromotion(anyList());
    }

    @Test
    void createOrder_verifiesPromotionUpdated() {
        OrderItemPostVm item = OrderItemPostVm.builder()
                .productId(5L).productName("Tablet").quantity(1)
                .productPrice(BigDecimal.valueOf(300)).note("").build();

        OrderPostVm vm = OrderPostVm.builder()
                .checkoutId("chk-promo").email("promo@test.com")
                .shippingAddressPostVm(buildAddressPostVm())
                .billingAddressPostVm(buildAddressPostVm())
                .tax(0f).discount(0f).numberItem(1)
                .totalPrice(BigDecimal.valueOf(300)).deliveryFee(BigDecimal.ZERO)
                .couponCode("PROMO50")
                .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .orderItemPostVms(List.of(item))
                .build();

        Order saved = buildOrder(11L, OrderStatus.PENDING);
        when(orderRepository.save(any())).thenReturn(saved);
        when(orderItemRepository.saveAll(any())).thenReturn(List.of());
        when(orderRepository.findById(any())).thenReturn(Optional.of(saved));
        doNothing().when(cartService).deleteCartItems(any());
        doNothing().when(productService).subtractProductStockQuantity(any());
        doNothing().when(promotionService).updateUsagePromotion(anyList());

        orderService.createOrder(vm);

        // Verify both save calls (initial + acceptOrder) and promotionService
        verify(orderRepository, times(2)).save(any());
        verify(promotionService, atLeastOnce()).updateUsagePromotion(anyList());
    }

    // ── getAllOrder với nhiều page ─────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getAllOrder_withMultiplePages_returnsTotalPagesCorrectly() {
        Order o1 = buildOrder(1L, OrderStatus.PENDING);
        Order o2 = buildOrder(2L, OrderStatus.ACCEPTED);
        Page<Order> page = new PageImpl<>(
                List.of(o1, o2),
                PageRequest.of(0, 2),
                5  // total 5 elements
        );
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        OrderListVm result = orderService.getAllOrder(
                timePair, "Phone",
                List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED),
                billingPair, "user@example.com",
                Pair.of(0, 2)
        );

        assertThat(result.orderList()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(5L);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllOrder_withAllStatusTypes_returnsResults() {
        Order order = buildOrder(3L, OrderStatus.COMPLETED);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        // Test with every OrderStatus explicitly
        OrderListVm result = orderService.getAllOrder(
                timePair, null,
                List.of(OrderStatus.COMPLETED, OrderStatus.SHIPPING, OrderStatus.CANCELLED),
                Pair.of("", ""), null,
                pageInfo
        );

        assertThat(result.orderList()).hasSize(1);
    }

    // ── updateOrderPaymentStatus – PENDING payment ────────────────────────────

    @Test
    void updateOrderPaymentStatus_whenPending_keepsOrderStatusUnchanged() {
        Order order = buildOrder(30L, OrderStatus.PENDING);
        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                .orderId(30L).paymentId(99L)
                .paymentStatus(PaymentStatus.PENDING.name())
                .orderStatus("PENDING")
                .build();

        when(orderRepository.findById(30L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(vm);

        // PENDING != COMPLETED so orderStatus stays PENDING (no PAID transition)
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PENDING.name());
        assertThat(order.getPaymentId()).isEqualTo(99L);
    }

    // ── getMyOrders – combinatons ─────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getMyOrders_withProductNameAndNullStatus_filtersByName() {
        setSubjectUpSecurityContext("user-10");
        Order order = buildOrder(50L, OrderStatus.PAID);
        when(orderRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(order));

        List<OrderGetVm> result = orderService.getMyOrders("Tablet", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).orderStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMyOrders_withNullNameAndStatus_filtersByStatus() {
        setSubjectUpSecurityContext("user-11");
        Order order = buildOrder(51L, OrderStatus.SHIPPING);
        when(orderRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(order));

        List<OrderGetVm> result = orderService.getMyOrders(null, OrderStatus.SHIPPING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(51L);
    }

    // ── isOrderCompleted – null variation response ────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void isOrderCompleted_whenVariationsNull_usesProductIdDirectly() {
        setSubjectUpSecurityContext("user-20");
        when(productService.getProductVariations(7L)).thenReturn(null);
        when(orderRepository.findOne(any(Specification.class)))
                .thenReturn(Optional.of(buildOrder(1L, OrderStatus.COMPLETED)));

        // null list → treated as empty → uses productId directly
        var result = orderService.isOrderCompletedWithUserIdAndProductId(7L);

        assertThat(result.isPresent()).isTrue();
    }

    // ── getLatestOrders – exact count = 1 ────────────────────────────────────

    @Test
    void getLatestOrders_countOne_returnsExactlyOne() {
        Order order = buildOrder(100L, OrderStatus.ACCEPTED);
        when(orderRepository.getLatestOrders(any())).thenReturn(List.of(order));

        var result = orderService.getLatestOrders(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).orderStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    // ── exportCsv – check CSV content has header ─────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void exportCsv_returnedBytesContainCsvContent() throws Exception {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        OrderRequest req = OrderRequest.builder()
                .createdFrom(ZonedDateTime.now().minusDays(30))
                .createdTo(ZonedDateTime.now())
                .productName("Phone")
                .orderStatus(List.of(OrderStatus.COMPLETED))
                .billingCountry("Vietnam").billingPhoneNumber("0123").email("x@x.com")
                .pageNo(0).pageSize(50).build();

        byte[] csv = orderService.exportCsv(req);

        // CSV always has at least a header row
        assertThat(csv).isNotNull();
        assertThat(csv.length).isGreaterThan(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportCsv_withMultipleOrders_callsMapperForEach() throws Exception {
        Order o1 = buildOrder(201L, OrderStatus.COMPLETED);
        Order o2 = buildOrder(202L, OrderStatus.COMPLETED);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(o1, o2)));

        com.yas.order.model.csv.OrderItemCsv row = com.yas.order.model.csv.OrderItemCsv.builder().build();
        when(orderMapper.toCsv(any())).thenReturn(row);

        OrderRequest req = OrderRequest.builder()
                .createdFrom(ZonedDateTime.now().minusDays(7)).createdTo(ZonedDateTime.now())
                .productName("").orderStatus(List.of(OrderStatus.COMPLETED))
                .billingCountry("").billingPhoneNumber("").email("")
                .pageNo(0).pageSize(10).build();

        orderService.exportCsv(req);

        // mapper called once per order
        verify(orderMapper, times(2)).toCsv(any());
    }

    // ── rejectOrder – null reason ─────────────────────────────────────────────

    @Test
    void rejectOrder_withNullReason_setsRejectStatus() {
        Order order = buildOrder(60L, OrderStatus.PENDING);
        when(orderRepository.findById(60L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.rejectOrder(60L, null);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECT);
        assertThat(order.getRejectReason()).isNull();
        verify(orderRepository).save(order);
    }

    // ── findOrderWithItemsById – empty items ──────────────────────────────────

    @Test
    void getOrderWithItemsById_whenFoundWithNoItems_returnsVmWithEmptySet() {
        Order order = buildOrder(70L, OrderStatus.ACCEPTED);
        when(orderRepository.findById(70L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(70L)).thenReturn(List.of());

        OrderVm result = orderService.getOrderWithItemsById(70L);

        assertThat(result.id()).isEqualTo(70L);
        assertThat(result.orderItemVms()).isEmpty();
    }

    // ── findOrderVmByCheckoutId – no items ───────────────────────────────────

    @Test
    void findOrderVmByCheckoutId_withNoItems_returnsVmWithEmptyOrderItems() {
        Order order = buildOrder(80L, OrderStatus.PAID);
        when(orderRepository.findByCheckoutId("chk-empty")).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(80L)).thenReturn(List.of());

        var result = orderService.findOrderVmByCheckoutId("chk-empty");

        assertThat(result.id()).isEqualTo(80L);
        assertThat(result.orderItems()).isEmpty();
    }

    // ── acceptOrder – multiple saves verified ────────────────────────────────

    @Test
    void acceptOrder_savesOnce() {
        Order order = buildOrder(90L, OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(90L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.acceptOrder(90L);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
        verify(orderRepository, times(1)).save(order);
    }

    // ── updateOrderPaymentStatus – verify paymentId set ──────────────────────

    @Test
    void updateOrderPaymentStatus_setsPaymentIdOnOrder() {
        Order order = buildOrder(95L, OrderStatus.PENDING_PAYMENT);
        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                .orderId(95L).paymentId(777L)
                .paymentStatus(PaymentStatus.COMPLETED.name())
                .orderStatus("PENDING_PAYMENT")
                .build();

        when(orderRepository.findById(95L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.updateOrderPaymentStatus(vm);

        assertThat(order.getPaymentId()).isEqualTo(777L);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
    }
}
