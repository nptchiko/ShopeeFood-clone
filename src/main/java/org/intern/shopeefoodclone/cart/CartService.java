package org.intern.shopeefoodclone.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.intern.shopeefoodclone.infras.cache.CacheService;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItem;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemRepository;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CartService {

    CacheService cacheService;
    MenuItemRepository menuItemRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    static String CART_PREFIX = "cart:";
    static Duration CART_TTL = Duration.ofMinutes(30);

    public Cart getCart(String userId) {
        String json = cacheService.get(CART_PREFIX + userId);
        return deserializeCart(json, userId);
    }

    public Cart addItemToCart(String userId, CartItemRequest request) {

        if (request.quantity() <= 0 || request.quantity() > 100)
            throw new AppException(ErrorCode.INVALID_INPUT, "Cart item's quantity is not valid");

        Cart cart = getCart(userId);

        UUID itemId = UUID.fromString(request.itemId());

        MenuItem menuItem = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_ITEM_NOT_FOUND));

        if (Boolean.FALSE.equals(menuItem.getIsAvailable())) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }

        String itemRestaurantId = menuItem.getCategory().getRestaurant().getId().toString();
        String itemRestaurantName = menuItem.getCategory().getRestaurant().getName();

        List<CartItem> updatedItems = new ArrayList<>();
        if (cart.items() != null) {
            updatedItems.addAll(cart.items());
        }

        if (cart.restaurantId() != null && !cart.restaurantId().equals(itemRestaurantId)) {
            throw new AppException(ErrorCode.DIFFERENT_RESTAURANT_IN_CART);
        }

        String restaurantId = cart.restaurantId() != null ? cart.restaurantId() : itemRestaurantId;
        String restaurantName = cart.restaurantName() != null ? cart.restaurantName() : itemRestaurantName;

        boolean itemExists = false;
        for (int i = 0; i < updatedItems.size(); i++) {
            CartItem item = updatedItems.get(i);
            if (item.itemId().equals(request.itemId())) {
                updatedItems.set(i, CartItem.builder()
                        .itemId(item.itemId())
                        .itemName(item.itemName())
                        .quantity(item.quantity() + request.quantity())
                        .price(item.price())
                        .build());
                itemExists = true;
                break;
            }
        }

        if (!itemExists) {
            updatedItems.add(CartItem.builder()
                    .itemId(request.itemId())
                    .itemName(menuItem.getName())
                    .quantity(request.quantity())
                    .price(menuItem.getPrice())
                    .build());
        }

        BigDecimal totalAmount = calculateTotalAmount(updatedItems);

        Cart updatedCart = Cart.builder()
                .cartId(userId)
                .restaurantId(restaurantId)
                .restaurantName(restaurantName)
                .items(updatedItems)
                .totalAmount(totalAmount)
                .build();

        saveCart(updatedCart);
        return updatedCart;
    }

    public Cart updateItemQuantity(String userId, String itemId, CartItemUpdateRequest request) {

        if (request.quantity() < 0 || request.quantity() > 100)
            throw new AppException(ErrorCode.INVALID_INPUT, "Cart item's quantity is not valid");

        Cart cart = getCart(userId);

        List<CartItem> updatedItems = new ArrayList<>();
        if (cart.items() != null) {
            updatedItems.addAll(cart.items());
        }

        boolean found = false;
        for (int i = 0; i < updatedItems.size(); i++) {
            CartItem item = updatedItems.get(i);
            if (item.itemId().equals(itemId)) {
                found = true;
                if (request.quantity() <= 0) {
                    updatedItems.remove(i);
                } else {
                    updatedItems.set(i, CartItem.builder()
                            .itemId(item.itemId())
                            .itemName(item.itemName())
                            .quantity(request.quantity())
                            .price(item.price())
                            .build());
                }
                break;
            }
        }

        if (!found) {
            throw new AppException(ErrorCode.MENU_ITEM_NOT_FOUND, "Item not found in cart");
        }

        String restaurantId = cart.restaurantId();
        String restaurantName = cart.restaurantName();

        if (updatedItems.isEmpty()) {
            restaurantId = null;
            restaurantName = null;
        }

        BigDecimal totalAmount = calculateTotalAmount(updatedItems);

        Cart updatedCart = Cart.builder()
                .cartId(userId)
                .restaurantId(restaurantId)
                .restaurantName(restaurantName)
                .items(updatedItems)
                .totalAmount(totalAmount)
                .build();

        saveCart(updatedCart);
        return updatedCart;
    }

    public Cart removeItemFromCart(String userId, String itemId) {
        Cart cart = getCart(userId);

        List<CartItem> updatedItems = new ArrayList<>();
        if (cart.items() != null) {
            updatedItems.addAll(cart.items());
        }

        boolean removed = updatedItems.removeIf(item -> item.itemId().equals(itemId));
        if (!removed) {
            throw new AppException(ErrorCode.MENU_ITEM_NOT_FOUND, "Item not found in cart");
        }

        String restaurantId = cart.restaurantId();
        String restaurantName = cart.restaurantName();

        if (updatedItems.isEmpty()) {
            restaurantId = null;
            restaurantName = null;
        }

        BigDecimal totalAmount = calculateTotalAmount(updatedItems);

        Cart updatedCart = Cart.builder()
                .cartId(userId)
                .restaurantId(restaurantId)
                .restaurantName(restaurantName)
                .items(updatedItems)
                .totalAmount(totalAmount)
                .build();

        saveCart(updatedCart);
        return updatedCart;
    }

    public void clearCart(String userId) {
        cacheService.delete(CART_PREFIX + userId);
    }

    private void saveCart(Cart cart) {
        String json = serializeCart(cart);
        cacheService.set(CART_PREFIX + cart.cartId(), json, CART_TTL.toSeconds());
    }

    private BigDecimal calculateTotalAmount(List<CartItem> items) {
        return items.stream()
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Cart deserializeCart(String json, String userId) {
        try {
            if (json == null || json.isBlank()) {
                return createEmptyCart(userId);
            }
            return objectMapper.readValue(json, Cart.class);
        } catch (Exception e) {
            log.error("Error deserializing cart for user {}", userId, e);
            return createEmptyCart(userId);
        }
    }

    private String serializeCart(Cart cart) {
        try {
            return objectMapper.writeValueAsString(cart);
        } catch (Exception e) {
            log.error("Error serializing cart", e);
            throw new AppException(ErrorCode.INVALID_INPUT, "Could not serialize cart data");
        }
    }

    private Cart createEmptyCart(String userId) {
        return Cart.builder()
                .cartId(userId)
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();
    }

    private void validateCart() {

    }
}
