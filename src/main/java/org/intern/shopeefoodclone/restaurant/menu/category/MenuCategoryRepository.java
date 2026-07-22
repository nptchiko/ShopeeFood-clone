package org.intern.shopeefoodclone.restaurant.menu.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID>, JpaSpecificationExecutor<MenuCategory> {
    int deleteAllByRestaurant_Id(UUID restaurantId);

    boolean existsByRestaurantIdAndName(UUID restaurantId, String name);

    List<MenuCategory> findByRestaurantIdOrderBySortOrderAsc(UUID restaurantId);
}
