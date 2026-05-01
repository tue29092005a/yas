package com.yas.cart.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.cart.model.CartItem;
import com.yas.cart.viewmodel.CartItemGetVm;
import com.yas.cart.viewmodel.CartItemPostVm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartItemMapperTest {

    private CartItemMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CartItemMapper();
    }

    // ── toGetVm ───────────────────────────────────────────────────────────────

    @Test
    void toGetVm_mapsAllFields() {
        CartItem cartItem = CartItem.builder()
                .customerId("user-1")
                .productId(42L)
                .quantity(5)
                .build();

        CartItemGetVm result = mapper.toGetVm(cartItem);

        assertThat(result.customerId()).isEqualTo("user-1");
        assertThat(result.productId()).isEqualTo(42L);
        assertThat(result.quantity()).isEqualTo(5);
    }

    // ── toCartItem(CartItemPostVm, userId) ────────────────────────────────────

    @Test
    void toCartItem_fromPostVm_mapsAllFields() {
        CartItemPostVm vm = CartItemPostVm.builder().productId(10L).quantity(3).build();

        CartItem result = mapper.toCartItem(vm, "user-abc");

        assertThat(result.getCustomerId()).isEqualTo("user-abc");
        assertThat(result.getProductId()).isEqualTo(10L);
        assertThat(result.getQuantity()).isEqualTo(3);
    }

    // ── toCartItem(userId, productId, quantity) ────────────────────────────────

    @Test
    void toCartItem_fromFields_mapsAllFields() {
        CartItem result = mapper.toCartItem("user-xyz", 99L, 7);

        assertThat(result.getCustomerId()).isEqualTo("user-xyz");
        assertThat(result.getProductId()).isEqualTo(99L);
        assertThat(result.getQuantity()).isEqualTo(7);
    }

    // ── toGetVms ──────────────────────────────────────────────────────────────

    @Test
    void toGetVms_mapsEmptyList() {
        List<CartItemGetVm> result = mapper.toGetVms(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void toGetVms_mapsMultipleItems() {
        CartItem item1 = CartItem.builder().customerId("u1").productId(1L).quantity(1).build();
        CartItem item2 = CartItem.builder().customerId("u1").productId(2L).quantity(2).build();

        List<CartItemGetVm> result = mapper.toGetVms(List.of(item1, item2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).productId()).isEqualTo(1L);
        assertThat(result.get(1).productId()).isEqualTo(2L);
        assertThat(result.get(1).quantity()).isEqualTo(2);
    }
}
