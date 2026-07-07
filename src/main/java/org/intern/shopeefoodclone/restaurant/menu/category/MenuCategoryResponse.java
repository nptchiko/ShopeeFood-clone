package org.intern.shopeefoodclone.restaurant.menu.category;

import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemResponse;

import java.util.List;
import java.util.UUID;

public record MenuCategoryResponse(
        UUID id,
        UUID restaurantId,
        String name,
        Integer sortOrder,
        List<MenuItemResponse> items
) {}
