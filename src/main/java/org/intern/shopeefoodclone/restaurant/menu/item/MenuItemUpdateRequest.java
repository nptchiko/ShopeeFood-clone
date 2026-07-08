package org.intern.shopeefoodclone.restaurant.menu.item;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MenuItemUpdateRequest(
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        String description,

        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        String imageUrl,

        Boolean isAvailable
) {}
