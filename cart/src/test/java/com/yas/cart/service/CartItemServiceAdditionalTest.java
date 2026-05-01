package com.yas.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.cart.mapper.CartItemMapper;
import com.yas.cart.model.CartItem;
import com.yas.cart.repository.CartItemRepository;
import com.yas.cart.viewmodel.CartItemDeleteVm;
import com.yas.cart.viewmodel.CartItemGetVm;
import com.yas.cart.viewmodel.CartItemPostVm;
import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class CartItemServiceAdditionalTest {

    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductService productService;
    @Spy  private CartItemMapper cartItemMapper = new CartItemMapper();

    @InjectMocks
    private CartItemService cartItemService;

    private static final String USER_ID = "user-123";
    private static final Long PRODUCT_ID = 10L;

    @BeforeEach
    void setUp() {
        Mockito.reset(cartItemRepository, productService);
    }

    private void mockCurrentUserId(String userId) {
        Jwt jwt = mock(Jwt.class);
        JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(jwtToken);
        when(jwt.getSubject()).thenReturn(userId);
        SecurityContextHolder.setContext(securityContext);
    }

    // ── deleteCartItem ────────────────────────────────────────────────────────

    @Nested
    class DeleteCartItemTest {

        @Test
        void deleteCartItem_callsRepositoryWithCorrectArgs() {
            mockCurrentUserId(USER_ID);
            doNothing().when(cartItemRepository).deleteByCustomerIdAndProductId(USER_ID, PRODUCT_ID);

            cartItemService.deleteCartItem(PRODUCT_ID);

            verify(cartItemRepository).deleteByCustomerIdAndProductId(USER_ID, PRODUCT_ID);
        }
    }

    // ── validateCartItemDeleteVms – edge cases ────────────────────────────────

    @Nested
    class ValidateCartItemDeleteVmsTest {

        @Test
        void deleteOrAdjust_whenDuplicateProductSameQuantity_doesNotThrow() {
            // Same productId & same quantity → no exception (existing qty == new qty)
            CartItemDeleteVm vm1 = new CartItemDeleteVm(PRODUCT_ID, 2);
            CartItemDeleteVm vm2 = new CartItemDeleteVm(PRODUCT_ID, 2);

            mockCurrentUserId(USER_ID);
            CartItem cartItem = CartItem.builder()
                    .customerId(USER_ID).productId(PRODUCT_ID).quantity(5).build();
            when(cartItemRepository.findByCustomerIdAndProductIdIn(any(), any()))
                    .thenReturn(List.of(cartItem));
            when(cartItemRepository.saveAll(any())).thenReturn(List.of(cartItem));

            // Should NOT throw – same productId same quantity is allowed
            List<CartItemGetVm> result = cartItemService.deleteOrAdjustCartItem(List.of(vm1, vm2));
            assertThat(result).isNotNull();
        }

        @Test
        void deleteOrAdjust_whenProductNotInCart_skipsIt() {
            // ProductId in the delete list but not present in the user's cart
            CartItemDeleteVm vm = new CartItemDeleteVm(999L, 1);

            mockCurrentUserId(USER_ID);
            // cartItemRepository returns empty → productId 999L not found in map
            when(cartItemRepository.findByCustomerIdAndProductIdIn(any(), any()))
                    .thenReturn(List.of());
            when(cartItemRepository.saveAll(any())).thenReturn(List.of());

            List<CartItemGetVm> result = cartItemService.deleteOrAdjustCartItem(List.of(vm));

            // No item matched → both lists empty → result is empty
            assertThat(result).isEmpty();
            verify(cartItemRepository).deleteAll(List.of());
            verify(cartItemRepository).saveAll(List.of());
        }

        @Test
        void deleteOrAdjust_whenDeleteQuantityEqualsCartQuantity_deletesItem() {
            // quantity == cartItem.quantity → should delete
            CartItem cartItem = CartItem.builder()
                    .customerId(USER_ID).productId(PRODUCT_ID).quantity(3).build();
            CartItemDeleteVm vm = new CartItemDeleteVm(PRODUCT_ID, 3); // equal

            mockCurrentUserId(USER_ID);
            when(cartItemRepository.findByCustomerIdAndProductIdIn(any(), any()))
                    .thenReturn(List.of(cartItem));
            when(cartItemRepository.saveAll(any())).thenReturn(List.of());

            List<CartItemGetVm> result = cartItemService.deleteOrAdjustCartItem(List.of(vm));

            verify(cartItemRepository).deleteAll(List.of(cartItem));
            assertThat(result).isEmpty();
        }

        @Test
        void deleteOrAdjust_withEmptyList_returnsEmpty() {
            List<CartItemGetVm> result = cartItemService.deleteOrAdjustCartItem(List.of());
            assertThat(result).isEmpty();
        }
    }

    // ── addCartItem – quantity accumulation ───────────────────────────────────

    @Nested
    class AddCartItemQuantityTest {

        @Test
        void addCartItem_whenExistingItem_accumulatesQuantityCorrectly() {
            CartItemPostVm vm = CartItemPostVm.builder().productId(PRODUCT_ID).quantity(5).build();
            CartItem existing = CartItem.builder()
                    .customerId(USER_ID).productId(PRODUCT_ID).quantity(3).build();

            mockCurrentUserId(USER_ID);
            when(productService.existsById(PRODUCT_ID)).thenReturn(true);
            when(cartItemRepository.findByCustomerIdAndProductId(anyString(), anyLong()))
                    .thenReturn(Optional.of(existing));
            when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CartItemGetVm result = cartItemService.addCartItem(vm);

            assertThat(result.quantity()).isEqualTo(8); // 3 + 5
        }

        @Test
        void addCartItem_newItem_setsQuantityFromVm() {
            CartItemPostVm vm = CartItemPostVm.builder().productId(PRODUCT_ID).quantity(7).build();

            mockCurrentUserId(USER_ID);
            when(productService.existsById(PRODUCT_ID)).thenReturn(true);
            when(cartItemRepository.findByCustomerIdAndProductId(anyString(), anyLong()))
                    .thenReturn(Optional.empty());
            when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CartItemGetVm result = cartItemService.addCartItem(vm);

            assertThat(result.quantity()).isEqualTo(7);
            assertThat(result.customerId()).isEqualTo(USER_ID);
            assertThat(result.productId()).isEqualTo(PRODUCT_ID);
        }
    }

    // ── updateCartItem ────────────────────────────────────────────────────────

    @Nested
    class UpdateCartItemTest {

        @Test
        void updateCartItem_replacesQuantity() {
            com.yas.cart.viewmodel.CartItemPutVm putVm = new com.yas.cart.viewmodel.CartItemPutVm(10);

            mockCurrentUserId(USER_ID);
            when(productService.existsById(PRODUCT_ID)).thenReturn(true);
            when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CartItemGetVm result = cartItemService.updateCartItem(PRODUCT_ID, putVm);

            assertThat(result.quantity()).isEqualTo(10);
            assertThat(result.customerId()).isEqualTo(USER_ID);
            assertThat(result.productId()).isEqualTo(PRODUCT_ID);
        }
    }

    // ── getCartItems ──────────────────────────────────────────────────────────

    @Nested
    class GetCartItemsTest {

        @Test
        void getCartItems_whenEmpty_returnsEmptyList() {
            mockCurrentUserId(USER_ID);
            when(cartItemRepository.findByCustomerIdOrderByCreatedOnDesc(USER_ID))
                    .thenReturn(List.of());

            List<CartItemGetVm> result = cartItemService.getCartItems();

            assertThat(result).isEmpty();
        }

        @Test
        void getCartItems_mapsAllItems() {
            CartItem item1 = CartItem.builder().customerId(USER_ID).productId(1L).quantity(1).build();
            CartItem item2 = CartItem.builder().customerId(USER_ID).productId(2L).quantity(2).build();

            mockCurrentUserId(USER_ID);
            when(cartItemRepository.findByCustomerIdOrderByCreatedOnDesc(USER_ID))
                    .thenReturn(List.of(item1, item2));

            List<CartItemGetVm> result = cartItemService.getCartItems();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).productId()).isEqualTo(1L);
            assertThat(result.get(1).productId()).isEqualTo(2L);
        }
    }

    // ── validateProduct ───────────────────────────────────────────────────────

    @Nested
    class ValidateProductTest {

        @Test
        void addCartItem_whenProductExists_doesNotThrow() {
            CartItemPostVm vm = CartItemPostVm.builder().productId(PRODUCT_ID).quantity(1).build();

            mockCurrentUserId(USER_ID);
            when(productService.existsById(PRODUCT_ID)).thenReturn(true);
            when(cartItemRepository.findByCustomerIdAndProductId(anyString(), anyLong()))
                    .thenReturn(Optional.empty());
            when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Should NOT throw
            CartItemGetVm result = cartItemService.addCartItem(vm);
            assertThat(result).isNotNull();
        }

        @Test
        void updateCartItem_whenProductNotFound_throwsNotFoundException() {
            when(productService.existsById(PRODUCT_ID)).thenReturn(false);

            assertThrows(NotFoundException.class,
                    () -> cartItemService.updateCartItem(PRODUCT_ID,
                            new com.yas.cart.viewmodel.CartItemPutVm(1)));
        }
    }

    // ── deleteOrAdjust – multiple items mixed ────────────────────────────────

    @Nested
    class DeleteOrAdjustMixedTest {

        @Test
        void deleteOrAdjust_multipleItems_someDeleteSomeAdjust() {
            Long productIdA = 1L;
            Long productIdB = 2L;

            CartItem itemA = CartItem.builder()
                    .customerId(USER_ID).productId(productIdA).quantity(2).build();
            CartItem itemB = CartItem.builder()
                    .customerId(USER_ID).productId(productIdB).quantity(10).build();

            // delete all of A (qty 2 >= 2), adjust B (qty 10 - 3 = 7)
            CartItemDeleteVm vmA = new CartItemDeleteVm(productIdA, 2);
            CartItemDeleteVm vmB = new CartItemDeleteVm(productIdB, 3);

            mockCurrentUserId(USER_ID);
            when(cartItemRepository.findByCustomerIdAndProductIdIn(any(), any()))
                    .thenReturn(List.of(itemA, itemB));
            when(cartItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            List<CartItemGetVm> result =
                    cartItemService.deleteOrAdjustCartItem(List.of(vmA, vmB));

            verify(cartItemRepository).deleteAll(List.of(itemA));
            assertThat(result).hasSize(1);
            assertThat(result.get(0).productId()).isEqualTo(productIdB);
            assertThat(result.get(0).quantity()).isEqualTo(7);
        }
    }
}
