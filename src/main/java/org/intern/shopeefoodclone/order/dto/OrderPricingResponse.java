package org.intern.shopeefoodclone.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderPricingResponse(
        UUID id,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal platformFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount
) {}
