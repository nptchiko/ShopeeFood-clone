package org.intern.shopeefoodclone.restaurant;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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
