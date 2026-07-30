package org.intern.shopeefoodclone.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.intern.shopeefoodclone.order.enums.DeliveryMethod;
import org.intern.shopeefoodclone.payment.PaymentMethodRequest;

import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        @NotNull(message = "Restaurant ID is required")
        UUID restaurantId,

        UUID deliveryAddressId,

        @NotNull(message = "Delivery method is required")
        DeliveryMethod deliveryMethod,

        String specialInstructions,

        @NotNull(message = "Payment method is required")
        @Valid
        PaymentMethodRequest paymentMethod,

        @Valid
        List<OrderItemCreateRequest> items
) {}
