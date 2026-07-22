package org.intern.shopeefoodclone.restaurant;

import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryResponse;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RestaurantMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Restaurant toEntity(RestaurantCreateRequest request);

    @Mapping(source = "owner.id", target = "ownerId")
    @Mapping(source = "address.id", target = "addressId")
    RestaurantResponse toResponse(Restaurant restaurant);

    List<RestaurantResponse> toResponseList(List<Restaurant> restaurants);

    default RestaurantDetailResponse toDetailResponse(Restaurant restaurant, List<MenuCategoryResponse> menu) {
        if (restaurant == null) {
            return null;
        }
        return new RestaurantDetailResponse(
                toResponse(restaurant),
                menu != null ? menu : Collections.emptyList()
        );
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(@MappingTarget Restaurant existing, RestaurantUpdateRequest request);
}
