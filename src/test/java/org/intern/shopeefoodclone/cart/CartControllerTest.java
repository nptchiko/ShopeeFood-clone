package org.intern.shopeefoodclone.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intern.shopeefoodclone.shared.exception.GlobalExceptionalHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private ObjectMapper objectMapper;
    private final String userId = "user-123";
    private String itemId;
    private Cart cart;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionalHandler())
                .build();

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(userId);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        itemId = UUID.randomUUID().toString();
        cart = Cart.builder()
                .cartId(userId)
                .restaurantId(UUID.randomUUID().toString())
                .restaurantName("KFC")
                .items(List.of(new CartItem(itemId, "Fried Chicken", 2, BigDecimal.valueOf(50000))))
                .totalAmount(BigDecimal.valueOf(100000))
                .build();
    }

    @Test
    void testGetCart_Success() throws Exception {
        when(cartService.getCart(userId)).thenReturn(cart);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.cartId").value(userId))
                .andExpect(jsonPath("$.data.restaurantName").value("KFC"))
                .andExpect(jsonPath("$.data.totalAmount").value(100000));

        verify(cartService).getCart(userId);
    }

    @Test
    void testAddItemToCart_Success() throws Exception {
        CartItemRequest request = new CartItemRequest(itemId, 2);
        when(cartService.addItemToCart(eq(userId), any(CartItemRequest.class))).thenReturn(cart);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Item added to cart successfully"));

        verify(cartService).addItemToCart(eq(userId), any(CartItemRequest.class));
    }

    @Test
    void testUpdateItemQuantity_Success() throws Exception {
        CartItemUpdateRequest request = new CartItemUpdateRequest(5);
        when(cartService.updateItemQuantity(eq(userId), eq(itemId), any(CartItemUpdateRequest.class))).thenReturn(cart);

        mockMvc.perform(put("/api/cart/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Cart item updated successfully"));

        verify(cartService).updateItemQuantity(eq(userId), eq(itemId), any(CartItemUpdateRequest.class));
    }

    @Test
    void testRemoveItem_Success() throws Exception {
        when(cartService.removeItemFromCart(userId, itemId)).thenReturn(cart);

        mockMvc.perform(delete("/api/cart/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Item removed from cart successfully"));

        verify(cartService).removeItemFromCart(userId, itemId);
    }

    @Test
    void testClearCart_Success() throws Exception {
        doNothing().when(cartService).clearCart(userId);

        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Cart cleared successfully"));

        verify(cartService).clearCart(userId);
    }
}
