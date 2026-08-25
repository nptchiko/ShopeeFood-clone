package org.intern.shopeefoodclone.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.order.dto.OrderCreateRequest;
import org.intern.shopeefoodclone.order.dto.OrderResponse;
import org.intern.shopeefoodclone.order.dto.OrderUpdateRequest;
import org.intern.shopeefoodclone.shared.api.ApiResponse;
import org.intern.shopeefoodclone.shared.api.PageResponse;
import org.intern.shopeefoodclone.shared.utils.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class OrderController {

    OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.created(orderService.create(request), "Order placed successfully");
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getAll(
            @RequestParam(required = false) String filter,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return ApiResponse.success(orderService.findAll(filter, pageable), "Orders retrieved successfully");
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancelOrder(@PathVariable UUID id){
        orderService.cancelOrder(id);
        return ApiResponse.success("Order cancelled successfully");
    }

    @GetMapping("/history")
    public ApiResponse<PageResponse<OrderResponse>> getHistory(
            @PageableDefault(sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) String filter
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(orderService.getOrderHistory(userId, filter, pageable));
    }

    // BASIC CRUD
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(orderService.getById(id), "Order retrieved successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<OrderResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody OrderUpdateRequest request) {
        return ApiResponse.success(orderService.update(id, request), "Order updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        orderService.delete(id);
        return ApiResponse.success("Order deleted successfully");
    }
}
