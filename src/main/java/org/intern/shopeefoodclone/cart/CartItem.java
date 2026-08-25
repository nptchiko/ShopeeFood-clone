package org.intern.shopeefoodclone.cart;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CartItem(
     String itemId,
     String itemName,
     int quantity,
     BigDecimal price
) {
}
