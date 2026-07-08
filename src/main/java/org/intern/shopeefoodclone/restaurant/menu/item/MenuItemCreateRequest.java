package org.intern.shopeefoodclone.restaurant.menu.item;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MenuItemCreateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        String imageUrl,

        Boolean isAvailable
) {}
