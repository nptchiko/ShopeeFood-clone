package org.intern.shopeefoodclone.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event published when an order is cancelled.
 * Topic: order.cancelled
 */
@Value
@Builder
public class OrderCancelledEvent {

    UUID correlationId;
    UUID orderId;
    UUID userId;
    UUID restaurantId;
    OffsetDateTime cancelledAt;

    @JsonCreator
    public OrderCancelledEvent(
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("orderId") UUID orderId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("restaurantId") UUID restaurantId,
            @JsonProperty("cancelledAt") OffsetDateTime cancelledAt
    ) {
        this.correlationId = correlationId;
        this.orderId = orderId;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.cancelledAt = cancelledAt;
    }
}
