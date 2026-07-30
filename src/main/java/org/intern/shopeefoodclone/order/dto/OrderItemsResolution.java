package org.intern.shopeefoodclone.order.dto;

import org.intern.shopeefoodclone.order.OrderItem;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemsResolution(
    List<OrderItem> items,
    BigDecimal subtotal,
    boolean orderedFromCart
) {}
