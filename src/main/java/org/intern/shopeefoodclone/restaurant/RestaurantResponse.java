package org.intern.shopeefoodclone.restaurant;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RestaurantResponse(
        UUID id,
        UUID ownerId,
        String name,
        UUID addressId,
        String description,
        String logoUrl,
        String bannerUrl,
        BigDecimal rating,
        Boolean isOpen,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
