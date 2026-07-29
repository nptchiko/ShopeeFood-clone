package org.intern.shopeefoodclone.cart;

import lombok.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Builder
public record Cart(
        String cartId,
        String restaurantId,
        String restaurantName,
        List<CartItem> items,
        BigDecimal totalAmount
) implements Serializable
{}
