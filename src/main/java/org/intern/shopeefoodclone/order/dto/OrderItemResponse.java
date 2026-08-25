package org.intern.shopeefoodclone.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderItemResponse(
        UUID id,
        UUID menuItemId,
        String itemName,
        Integer quantity,
        BigDecimal unitPrice,
        String specialNotes
) {}
