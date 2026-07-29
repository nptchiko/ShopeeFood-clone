package org.intern.shopeefoodclone.cart;

import org.intern.shopeefoodclone.infras.cache.CacheService;
import org.intern.shopeefoodclone.restaurant.Restaurant;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategory;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItem;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemRepository;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private MenuItemRepository menuItemRepository;

    private ObjectMapper objectMapper;
    private CartService cartService;

    private final String userId = "user-123";
    private UUID itemId;
    private UUID restaurantId;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cartService = new CartService(cacheService, menuItemRepository, objectMapper);
        itemId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .id(restaurantId)
                .name("KFC")
                .build();

        MenuCategory category = MenuCategory.builder()
                .restaurant(restaurant)
                .build();

        menuItem = MenuItem.builder()
                .id(itemId)
                .name("Fried Chicken")
                .price(BigDecimal.valueOf(50000))
                .category(category)
                .isAvailable(true)
                .build();
    }

    @Test
    void testGetCart_Empty() {
        when(cacheService.get("cart:" + userId)).thenReturn(null);

        Cart cart = cartService.getCart(userId);

        assertNotNull(cart);
        assertEquals(userId, cart.cartId());
        assertTrue(cart.items().isEmpty());
        assertEquals(BigDecimal.ZERO, cart.totalAmount());
    }

    @Test
    void testGetCart_Exists() throws Exception {
        Cart existingCart = Cart.builder()
                .cartId(userId)
                .restaurantId(restaurantId.toString())
                .restaurantName("KFC")
                .items(List.of(new CartItem(itemId.toString(), "Fried Chicken", 2, BigDecimal.valueOf(50000))))
                .totalAmount(BigDecimal.valueOf(100000))
                .build();
        String json = objectMapper.writeValueAsString(existingCart);
        when(cacheService.get("cart:" + userId)).thenReturn(json);

        Cart cart = cartService.getCart(userId);

        assertNotNull(cart);
        assertEquals(userId, cart.cartId());
        assertEquals(1, cart.items().size());
        assertEquals(BigDecimal.valueOf(100000), cart.totalAmount());
    }

    @Test
    void testAddItemToCart_Success_NewItem() {
        when(cacheService.get("cart:" + userId)).thenReturn(null);
        when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(menuItem));

        CartItemRequest request = new CartItemRequest(itemId.toString(), 2);
        Cart cart = cartService.addItemToCart(userId, request);

        assertNotNull(cart);
        assertEquals(restaurantId.toString(), cart.restaurantId());
        assertEquals("KFC", cart.restaurantName());
        assertEquals(1, cart.items().size());
        assertEquals(2, cart.items().getFirst().quantity());
        assertEquals(BigDecimal.valueOf(100000), cart.totalAmount());

        verify(cacheService).set(eq("cart:" + userId), anyString(), anyLong());
    }

    @Test
    void testAddItemToCart_Success_IncrementQuantity() throws Exception {
        Cart existingCart = Cart.builder()
                .cartId(userId)
                .restaurantId(restaurantId.toString())
                .restaurantName("KFC")
                .items(new ArrayList<>(List.of(new CartItem(itemId.toString(), "Fried Chicken", 1, BigDecimal.valueOf(50000)))))
                .totalAmount(BigDecimal.valueOf(50000))
                .build();
        String json = objectMapper.writeValueAsString(existingCart);
        when(cacheService.get("cart:" + userId)).thenReturn(json);
        when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(menuItem));

        CartItemRequest request = new CartItemRequest(itemId.toString(), 2);
        Cart cart = cartService.addItemToCart(userId, request);

        assertNotNull(cart);
        assertEquals(1, cart.items().size());
        assertEquals(3, cart.items().getFirst().quantity());
        assertEquals(BigDecimal.valueOf(150000), cart.totalAmount());
    }

    @Test
    void testAddItemToCart_DifferentRestaurant_ThrowsException() throws Exception {
        Cart existingCart = Cart.builder()
                .cartId(userId)
                .restaurantId(UUID.randomUUID().toString())
                .restaurantName("McDonald's")
                .items(new ArrayList<>(List.of(new CartItem(UUID.randomUUID().toString(), "Burger", 1, BigDecimal.valueOf(60000)))))
                .totalAmount(BigDecimal.valueOf(60000))
                .build();
        String json = objectMapper.writeValueAsString(existingCart);
        when(cacheService.get("cart:" + userId)).thenReturn(json);
        when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(menuItem));

        CartItemRequest request = new CartItemRequest(itemId.toString(), 1);

        AppException exception = assertThrows(AppException.class, () -> cartService.addItemToCart(userId, request));
        assertEquals(ErrorCode.DIFFERENT_RESTAURANT_IN_CART, exception.getErrorCode());
    }

    @Test
    void testAddItemToCart_ItemUnavailable_ThrowsException() {
        menuItem.setIsAvailable(false);
        when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(menuItem));

        CartItemRequest request = new CartItemRequest(itemId.toString(), 1);

        AppException exception = assertThrows(AppException.class, () -> cartService.addItemToCart(userId, request));
        assertEquals(ErrorCode.OUT_OF_STOCK, exception.getErrorCode());
    }

    @Test
    void testUpdateItemQuantity_Success() throws Exception {
        Cart existingCart = Cart.builder()
                .cartId(userId)
                .restaurantId(restaurantId.toString())
                .restaurantName("KFC")
                .items(new ArrayList<>(List.of(new CartItem(itemId.toString(), "Fried Chicken", 2, BigDecimal.valueOf(50000)))))
                .totalAmount(BigDecimal.valueOf(100000))
                .build();
        String json = objectMapper.writeValueAsString(existingCart);
        when(cacheService.get("cart:" + userId)).thenReturn(json);

        CartItemUpdateRequest request = new CartItemUpdateRequest(5);
        Cart cart = cartService.updateItemQuantity(userId, itemId.toString(), request);

        assertNotNull(cart);
        assertEquals(5, cart.items().getFirst().quantity());
        assertEquals(BigDecimal.valueOf(250000), cart.totalAmount());
    }

    @Test
    void testUpdateItemQuantity_Zero_RemovesItem() throws Exception {
        Cart existingCart = Cart.builder()
                .cartId(userId)
                .restaurantId(restaurantId.toString())
                .restaurantName("KFC")
                .items(new ArrayList<>(List.of(new CartItem(itemId.toString(), "Fried Chicken", 2, BigDecimal.valueOf(50000)))))
                .totalAmount(BigDecimal.valueOf(100000))
                .build();
        String json = objectMapper.writeValueAsString(existingCart);
        when(cacheService.get("cart:" + userId)).thenReturn(json);

        CartItemUpdateRequest request = new CartItemUpdateRequest(0);
        Cart cart = cartService.updateItemQuantity(userId, itemId.toString(), request);

        assertNotNull(cart);
        assertTrue(cart.items().isEmpty());
        assertNull(cart.restaurantId());
        assertNull(cart.restaurantName());
        assertEquals(BigDecimal.ZERO, cart.totalAmount());
    }

    @Test
    void testRemoveItem_Success() throws Exception {
        Cart existingCart = Cart.builder()
                .cartId(userId)
                .restaurantId(restaurantId.toString())
                .restaurantName("KFC")
                .items(new ArrayList<>(List.of(new CartItem(itemId.toString(), "Fried Chicken", 2, BigDecimal.valueOf(50000)))))
                .totalAmount(BigDecimal.valueOf(100000))
                .build();
        String json = objectMapper.writeValueAsString(existingCart);
        when(cacheService.get("cart:" + userId)).thenReturn(json);

        Cart cart = cartService.removeItemFromCart(userId, itemId.toString());

        assertNotNull(cart);
        assertTrue(cart.items().isEmpty());
        assertNull(cart.restaurantId());
        assertEquals(BigDecimal.ZERO, cart.totalAmount());
    }

    @Test
    void testClearCart() {
        cartService.clearCart(userId);
        verify(cacheService).delete("cart:" + userId);
    }
}
