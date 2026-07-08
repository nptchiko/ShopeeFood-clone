package org.intern.shopeefoodclone.restaurant.menu.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID>, JpaSpecificationExecutor<MenuItem> {
    List<MenuItem> findByCategoryId(UUID categoryId);

    boolean existsByCategoryIdAndName(UUID categoryId, String name);

    List<MenuItem> findByCategoryIdIn(List<UUID> attr0);
}
