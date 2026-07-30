package org.intern.shopeefoodclone.order;

import org.intern.shopeefoodclone.order.dto.OrderCreateRequest;
import org.intern.shopeefoodclone.order.dto.OrderItemResponse;
import org.intern.shopeefoodclone.order.dto.OrderPricingResponse;
import org.intern.shopeefoodclone.order.dto.OrderResponse;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "deliveryAddress", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "pricing", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(OrderCreateRequest request);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "restaurant.name", target = "restaurantName")
    @Mapping(source = "deliveryAddress.id", target = "deliveryAddressId")
    @Mapping(expression = "java(entity.getDeliveryAddress() != null ? (entity.getDeliveryAddress().getLine1() + (entity.getDeliveryAddress().getLine2() != null && !entity.getDeliveryAddress().getLine2().isBlank() ? \", \" + entity.getDeliveryAddress().getLine2() : \"\") + \", \" + entity.getDeliveryAddress().getCity()) : null)", target = "deliveryAddressLine")
    OrderResponse toResponse(Order entity);

    List<OrderResponse> toResponseList(List<Order> entities);

    @Mapping(source = "menuItem.id", target = "menuItemId")
    @Mapping(source = "menuItem.name", target = "itemName")
    OrderItemResponse toItemResponse(OrderItem entity);

    OrderPricingResponse toPricingResponse(OrderPricing entity);
}
