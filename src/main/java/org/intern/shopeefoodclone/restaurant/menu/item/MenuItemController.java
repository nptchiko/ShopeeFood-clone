package org.intern.shopeefoodclone.restaurant.menu.item;

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
public class MenuItemController {

    MenuItemService menuItemService;

    @PostMapping("/api/categories/{categoryId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MenuItemResponse> create(
            @PathVariable UUID categoryId,
            @Valid @RequestBody MenuItemCreateRequest request
    ) {
        return ApiResponse.created(menuItemService.create(categoryId, request), "Menu item created successfully");
    }

    @GetMapping("/api/categories/{categoryId}/items")
    public ApiResponse<PageResponse<MenuItemResponse>> getByCategoryId(
            @PathVariable UUID categoryId,
            @RequestParam(required = false) String filter,
            Pageable pageable) {
        return ApiResponse.success(menuItemService.findByCategoryId(categoryId, filter, pageable), "Menu items retrieved successfully");
    }

    @GetMapping("/api/items/{id}")
    public ApiResponse<MenuItemResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(menuItemService.getById(id), "Menu item retrieved successfully");
    }

    @PutMapping("/api/items/{id}")
    public ApiResponse<MenuItemResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody MenuItemUpdateRequest request
    ) {
        return ApiResponse.success(menuItemService.update(id, request), "Menu item updated successfully");
    }

    @DeleteMapping("/api/items/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        menuItemService.delete(id);
        return ApiResponse.success("Menu item deleted successfully");
    }
}
