package org.intern.shopeefoodclone.restaurant;

import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RestaurantDetailResponse(
        UUID id,
        UUID ownerId,
        String name,
        UUID addressId,
        String description,
        String logoUrl,
        String bannerUrl,
        BigDecimal rating,
        Boolean isOpen,
        List<MenuCategoryResponse> menu
) {}
