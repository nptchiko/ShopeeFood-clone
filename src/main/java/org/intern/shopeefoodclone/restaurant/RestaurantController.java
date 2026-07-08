package org.intern.shopeefoodclone.restaurant;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.shared.api.ApiResponse;
import org.intern.shopeefoodclone.shared.api.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class RestaurantController {

    RestaurantService restaurantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RestaurantResponse> create(@Valid @RequestBody RestaurantCreateRequest request) {
        return ApiResponse.created(restaurantService.create(request), "Restaurant created successfully");
    }

    @GetMapping
    public ApiResponse<PageResponse<RestaurantResponse>> getAll(
            @RequestParam(required = false) String filter,
            @PageableDefault(sort = "name") Pageable pageable) {
        return ApiResponse.success(restaurantService.findAll(filter, pageable), "Restaurants retrieved successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<RestaurantDetailResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(restaurantService.getById(id), "Restaurant retrieved successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<RestaurantResponse> update(@PathVariable UUID id, @Valid @RequestBody RestaurantUpdateRequest request) {
        return ApiResponse.success(restaurantService.update(id, request), "Restaurant updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        restaurantService.delete(id);
        return ApiResponse.success("Restaurant deleted successfully");
    }
}
