package org.intern.shopeefoodclone.restaurant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RestaurantCreateRequest(
        @NotNull(message = "Owner ID is required")
        UUID ownerId,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        String description,
        String logoUrl,
        String bannerUrl,
        Boolean isOpen
) {}
