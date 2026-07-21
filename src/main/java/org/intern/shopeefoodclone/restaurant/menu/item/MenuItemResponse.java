package org.intern.shopeefoodclone.restaurant.menu.item;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MenuItemResponse(
        UUID id,
        UUID categoryId,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        Boolean isAvailable
) {}
