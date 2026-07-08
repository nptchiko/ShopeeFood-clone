package org.intern.shopeefoodclone.restaurant.menu.category;

import jakarta.validation.constraints.Size;

public record MenuCategoryUpdateRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        Integer sortOrder
) {}
