package org.intern.shopeefoodclone.order.dto;

import jakarta.validation.constraints.NotNull;
import org.intern.shopeefoodclone.order.enums.OrderStatus;

public record OrderUpdateRequest(
        @NotNull(message = "Status is required")
        OrderStatus status
) {}
