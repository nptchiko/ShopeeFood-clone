package org.intern.shopeefoodclone.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.intern.shopeefoodclone.order.enums.DeliveryMethod;
import org.intern.shopeefoodclone.order.enums.OrderStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(
        UUID id,
        UUID userId,
        UUID restaurantId,
        String restaurantName,
        UUID deliveryAddressId,
        String deliveryAddressLine,
        DeliveryMethod deliveryMethod,
        OrderStatus status,
        String specialInstructions,
        OrderPricingResponse pricing,
        List<OrderItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
