package org.intern.shopeefoodclone.restaurant.menu.category;

import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemResponse;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuCategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    MenuCategory toEntity(MenuCategoryCreateRequest request);

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(target = "items", expression = "java(java.util.Collections.emptyList())")
    MenuCategoryResponse toResponse(MenuCategory menuCategory);

    List<MenuCategoryResponse> toResponseList(List<MenuCategory> categories);

    default MenuCategoryResponse toResponseWithItems(MenuCategory menuCategory, List<MenuItemResponse> items) {
        if (menuCategory == null) {
            return null;
        }
        return new MenuCategoryResponse(
                menuCategory.getId(),
                menuCategory.getRestaurant() != null ? menuCategory.getRestaurant().getId() : null,
                menuCategory.getName(),
                menuCategory.getSortOrder(),
                items != null ? items : Collections.emptyList()
        );
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(@MappingTarget MenuCategory existing, MenuCategoryUpdateRequest request);
}
