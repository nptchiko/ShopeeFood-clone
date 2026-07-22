package org.intern.shopeefoodclone.restaurant;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryResponse;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RestaurantDetailResponse(
        RestaurantResponse restaurant,
        List<MenuCategoryResponse> menu
) {}
