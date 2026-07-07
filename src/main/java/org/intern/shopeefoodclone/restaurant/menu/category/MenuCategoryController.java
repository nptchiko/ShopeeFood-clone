package org.intern.shopeefoodclone.restaurant.menu.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.shared.api.ApiResponse;
import org.intern.shopeefoodclone.shared.api.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class MenuCategoryController {

    MenuCategoryService menuCategoryService;

    @PostMapping("/api/restaurants/{restaurantId}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MenuCategoryResponse> create(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody MenuCategoryCreateRequest request
    ) {
        return ApiResponse.created(menuCategoryService.create(restaurantId, request), "Menu category created successfully");
    }

    @GetMapping("/api/restaurants/{restaurantId}/categories")
    public ApiResponse<PageResponse<MenuCategoryResponse>> getByRestaurantId(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) String filter,
            Pageable pageable) {
        return ApiResponse.success(menuCategoryService.findByRestaurantId(restaurantId, filter, pageable), "Menu categories retrieved successfully");
    }

    @GetMapping("/api/categories/{id}")
    public ApiResponse<MenuCategoryResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(menuCategoryService.getById(id), "Menu category retrieved successfully");
    }

    @PutMapping("/api/categories/{id}")
    public ApiResponse<MenuCategoryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody MenuCategoryUpdateRequest request
    ) {
        return ApiResponse.success(menuCategoryService.update(id, request), "Menu category updated successfully");
    }

    @DeleteMapping("/api/categories/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        menuCategoryService.delete(id);
        return ApiResponse.success("Menu category deleted successfully");
    }
}
