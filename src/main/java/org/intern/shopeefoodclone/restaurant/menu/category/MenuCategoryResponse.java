package org.intern.shopeefoodclone.restaurant.menu.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemResponse;

import java.util.List;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MenuCategoryResponse(
        UUID id,
        UUID restaurantId,
        String name,
        Integer sortOrder,
        List<MenuItemResponse> items
) {}
