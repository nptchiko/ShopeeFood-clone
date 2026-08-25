package org.intern.shopeefoodclone.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event published when an order is placed.
 * Topic: order.placed
 */
@Value
@Builder
public class OrderPlacedEvent {

    UUID correlationId;
    UUID orderId;
    UUID userId;
    UUID restaurantId;
    BigDecimal totalAmount;
    OffsetDateTime placedAt;

    @JsonCreator
    public OrderPlacedEvent(
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("orderId") UUID orderId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("restaurantId") UUID restaurantId,
            @JsonProperty("totalAmount") BigDecimal totalAmount,
            @JsonProperty("placedAt") OffsetDateTime placedAt
    ) {
        this.correlationId = correlationId;
        this.orderId = orderId;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.totalAmount = totalAmount;
        this.placedAt = placedAt;
    }
}
