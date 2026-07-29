package org.intern.shopeefoodclone.cart;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.shared.api.ApiResponse;
import org.intern.shopeefoodclone.shared.utils.SecurityUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class CartController {

    CartService cartService;

    @GetMapping
    public ApiResponse<Cart> getCart() {
        String userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartService.getCart(userId);
        return ApiResponse.success(cart, "Cart retrieved successfully");
    }

    @PostMapping("/items")
    public ApiResponse<Cart> addItem(@Valid @RequestBody CartItemRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartService.addItemToCart(userId, request);
        return ApiResponse.success(cart, "Item added to cart successfully");
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<Cart> updateItemQuantity(
            @PathVariable String itemId,
            @Valid @RequestBody CartItemUpdateRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartService.updateItemQuantity(userId, itemId, request);
        return ApiResponse.success(cart, "Cart item updated successfully");
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Cart> removeItem(@PathVariable String itemId) {
        String userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartService.removeItemFromCart(userId, itemId);
        return ApiResponse.success(cart, "Item removed from cart successfully");
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart() {
        String userId = SecurityUtils.getCurrentUserId();
        cartService.clearCart(userId);
        return ApiResponse.success("Cart cleared successfully");
    }
}
