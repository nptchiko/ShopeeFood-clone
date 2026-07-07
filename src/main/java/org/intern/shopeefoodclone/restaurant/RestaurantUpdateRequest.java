package org.intern.shopeefoodclone.restaurant;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record RestaurantUpdateRequest(
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Size(max = 5, message = "Rating must be at most 5")
        BigDecimal rating,

        UUID addressId,
        String description,
        String logoUrl,
        String bannerUrl,
        Boolean isOpen
) {}
