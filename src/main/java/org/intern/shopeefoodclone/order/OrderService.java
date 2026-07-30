package org.intern.shopeefoodclone.order;

import io.github.perplexhub.rsql.RSQLJPASupport;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.intern.shopeefoodclone.cart.Cart;
import org.intern.shopeefoodclone.cart.CartItem;
import org.intern.shopeefoodclone.cart.CartService;
import org.intern.shopeefoodclone.infras.cache.CacheService;
import org.intern.shopeefoodclone.infras.messaging.KafkaEventPublisher;
import org.intern.shopeefoodclone.order.dto.*;
import org.intern.shopeefoodclone.order.enums.DeliveryMethod;
import org.intern.shopeefoodclone.order.enums.OrderStatus;
import org.intern.shopeefoodclone.payment.Payment;
import org.intern.shopeefoodclone.payment.PaymentMethod;
import org.intern.shopeefoodclone.payment.PaymentRepository;
import org.intern.shopeefoodclone.payment.PaymentStatus;
import org.intern.shopeefoodclone.restaurant.Restaurant;
import org.intern.shopeefoodclone.restaurant.RestaurantRepository;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItem;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemRepository;
import org.intern.shopeefoodclone.shared.api.PageResponse;
import org.intern.shopeefoodclone.shared.constant.AppDate;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.intern.shopeefoodclone.shared.utils.PaginationUtils;
import org.intern.shopeefoodclone.shared.utils.SecurityUtils;
import org.intern.shopeefoodclone.user.User;
import org.intern.shopeefoodclone.user.UserRepository;
import org.intern.shopeefoodclone.user.address.Address;
import org.intern.shopeefoodclone.user.address.AddressRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class OrderService {

    OrderRepository orderRepository;
    UserRepository userRepository;
    RestaurantRepository restaurantRepository;
    AddressRepository addressRepository;
    MenuItemRepository menuItemRepository;
    PaymentRepository paymentRepository;
    CartService cartService;
    CacheService cacheService;
    OrderMapper orderMapper;
    KafkaEventPublisher kafkaEventPublisher;

    private static final BigDecimal BASE_DELIVERY_FEE = BigDecimal.valueOf(15000.00);
    private static final BigDecimal PLATFORM_FEE = BigDecimal.valueOf(2000.00);
    private static final double MAX_DELIVERY_DISTANCE = 15000.0;
    private static final double BASE_DISTANCE_M = 2000.0;
    private static final double EXTRA_FEE_PER_KM = 5000.0;

    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        String currentUserIdStr = SecurityUtils.getCurrentUserId();
        UUID userId = UUID.fromString(currentUserIdStr);

        // Idempotency Check
        String lockKey = "order:lock:" + currentUserIdStr;
        acquireLock(lockKey);

        try {
            User user = getUser(userId);
            Restaurant restaurant = getAndValidateRestaurant(request.restaurantId());

            Address deliveryAddress = resolveDeliveryAddress(request, userId);
            BigDecimal deliveryFee = calculateDeliveryFee(request, deliveryAddress, restaurant);

            OrderItemsResolution resolvedItems = resolveItems(request, restaurant, currentUserIdStr);

            OrderPricing pricing = buildPricing(resolvedItems.subtotal(), deliveryFee);

            Order savedOrder = buildAndSaveOrder(user, restaurant, deliveryAddress, request, resolvedItems.items(), pricing);

            //processPayment(savedOrder, request.paymentMethod(), pricing.getTotalAmount());

            if (resolvedItems.orderedFromCart()) {
                cartService.clearCart(currentUserIdStr);
            }

            kafkaEventPublisher.publishOrderPlaced(savedOrder);

            return orderMapper.toResponse(savedOrder);

        } finally {
            cacheService.delete(lockKey);
        }
    }

    private void acquireLock(String lockKey) {
        if (cacheService.hasKey(lockKey)) {
            throw new AppException(ErrorCode.INVALID_INPUT, "An order is already being processed. Please try again in a few seconds.");
        }
        cacheService.set(lockKey, "LOCKED", 10);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Restaurant getAndValidateRestaurant(UUID restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_NOT_FOUND));

        if (Boolean.FALSE.equals(restaurant.getIsOpen())) {
            throw new AppException(ErrorCode.RESTAURANT_CLOSED);
        }
        return restaurant;
    }

    private Address resolveDeliveryAddress(OrderCreateRequest request, UUID userId) {
        if (request.deliveryMethod() != DeliveryMethod.DELIVERY) {
            return null;
        }
        if (request.deliveryAddressId() == null) {
            throw new AppException(ErrorCode.DELIVERY_ADDRESS_NOT_FOUND);
        }
        Address deliveryAddress = addressRepository.findById(request.deliveryAddressId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        if (deliveryAddress.getUser() == null || !deliveryAddress.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "This address does not belong to the user.");
        }
        return deliveryAddress;
    }

    private BigDecimal calculateDeliveryFee(OrderCreateRequest request, Address deliveryAddress, Restaurant restaurant) {
        if (request.deliveryMethod() != DeliveryMethod.DELIVERY || deliveryAddress == null) {
            return BigDecimal.ZERO;
        }

        Address restAddress = restaurant.getAddress();
        if (restAddress == null || restAddress.getLatitude() == null || restAddress.getLongitude() == null ||
                deliveryAddress.getLatitude() == null || deliveryAddress.getLongitude() == null) {
            return BASE_DELIVERY_FEE;
        }

        double distance = calculateDistance(
                restAddress.getLatitude(), restAddress.getLongitude(),
                deliveryAddress.getLatitude(), deliveryAddress.getLongitude()
        );

        if (distance > MAX_DELIVERY_DISTANCE) {
            throw new AppException(ErrorCode.OUT_OF_DELIVERY_AREA,
                    String.format("Delivery distance (%.2f km) exceeds the 15km limit.", distance / 1000.0));
        }

        if (distance > BASE_DISTANCE_M) {
            double extraKm = (distance - BASE_DISTANCE_M) / 1000.0;
            BigDecimal additionalFee = BigDecimal.valueOf(Math.ceil(extraKm) * EXTRA_FEE_PER_KM);
            return BASE_DELIVERY_FEE.add(additionalFee);
        }

        return BASE_DELIVERY_FEE;
    }

    private OrderItemsResolution resolveItems(OrderCreateRequest request, Restaurant restaurant, String currentUserIdStr) {
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        // #1 Collect order items from request
        // OR
        // #2 Convert from cart

        if (request.items() != null && !request.items().isEmpty()) {
            for (OrderItemCreateRequest itemReq : request.items()) {
                MenuItem menuItem = getAndValidateMenuItem(itemReq.menuItemId(), restaurant);

                // subtotal += price * quantity
                subtotal = subtotal.add(menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));

                orderItems.add(buildOrderItem(menuItem, itemReq.quantity(), itemReq.specialNotes()));
            }
            return new OrderItemsResolution(orderItems, subtotal, false);
        } else {
            Cart cart = cartService.getCart(currentUserIdStr);
            if (cart.items() == null || cart.items().isEmpty()) {
                throw new AppException(ErrorCode.CART_IS_EMPTY);
            }

            if (cart.restaurantId() == null || !UUID.fromString(cart.restaurantId()).equals(restaurant.getId())) {
                throw new AppException(ErrorCode.DIFFERENT_RESTAURANT_IN_CART);
            }

            for (CartItem cartItem : cart.items()) {
                UUID itemId = UUID.fromString(cartItem.itemId());
                MenuItem menuItem = getAndValidateMenuItem(itemId, restaurant);
                subtotal = subtotal.add(menuItem.getPrice().multiply(BigDecimal.valueOf(cartItem.quantity())));
                orderItems.add(buildOrderItem(menuItem, cartItem.quantity(), null));
            }
            return new OrderItemsResolution(orderItems, subtotal, true);
        }
    }

    private MenuItem getAndValidateMenuItem(UUID menuItemId, Restaurant restaurant) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_ITEM_NOT_FOUND));

        if (Boolean.FALSE.equals(menuItem.getIsAvailable())) {
            throw new AppException(ErrorCode.OUT_OF_STOCK, "Item " + menuItem.getName() + " is out of stock.");
        }

        if (!menuItem.getCategory().getRestaurant().getId().equals(restaurant.getId())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Item " + menuItem.getName() + " does not belong to the selected restaurant.");
        }
        return menuItem;
    }

    private OrderItem buildOrderItem(MenuItem menuItem, int quantity, String specialNotes) {
        return OrderItem.builder()
                .menuItem(menuItem)
                .quantity(quantity)
                .unitPrice(menuItem.getPrice())
                .specialNotes(specialNotes)
                .build();
    }

    private OrderPricing buildPricing(BigDecimal subtotal, BigDecimal deliveryFee) {

        // add promotion later
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(deliveryFee).add(PLATFORM_FEE).subtract(discountAmount);

        return OrderPricing.builder()
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .platformFee(PLATFORM_FEE)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .build();
    }

    private Order buildAndSaveOrder(User user, Restaurant restaurant, Address deliveryAddress,
                                    OrderCreateRequest request, List<OrderItem> items, OrderPricing pricing) {
        Order order = Order.builder()
                .user(user)
                .restaurant(restaurant)
                .deliveryAddress(deliveryAddress)
                .deliveryMethod(request.deliveryMethod())
                .specialInstructions(request.specialInstructions())
                .status(OrderStatus.PENDING)
                .build();

        pricing.setOrder(order);
        order.setPricing(pricing);

        items.forEach(item -> item.setOrder(order));
        order.setItems(items);

        return orderRepository.save(order);
    }

    private void processPayment(Order savedOrder, org.intern.shopeefoodclone.payment.PaymentMethodRequest paymentRequest, BigDecimal totalAmount) {
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .type(paymentRequest.type())
                .partyName(paymentRequest.partyName())
                .gatewayToken(paymentRequest.gatewayToken())
                .build();

        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        OffsetDateTime paidAt = null;

        if (StringUtils.hasText(paymentMethod.getGatewayToken())) {
            paymentStatus = PaymentStatus.CAPTURED;
            paidAt = AppDate.now();
            savedOrder.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(savedOrder);
        }

        Payment payment = Payment.builder()
                .order(savedOrder)
                .amount(totalAmount)
                .paymentMethod(paymentMethod)
                .status(paymentStatus)
                .paidAt(paidAt)
                .build();

        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findAll(String filter, Pageable pageable) {
        Pageable boundedPageable = PaginationUtils.validateAndBound(pageable);
        Page<Order> page = StringUtils.hasText(filter)
                ? orderRepository.findAll(RSQLJPASupport.toSpecification(filter), boundedPageable)
                : orderRepository.findAll(boundedPageable);
        return PaginationUtils.toPageResponse(page, orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        return orderMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public OrderResponse update(UUID id, OrderUpdateRequest request) {
        Order order = findOrThrow(id);

        if (request.status() == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
                throw new AppException(ErrorCode.CANNOT_CANCEL_ORDER);
        }

        order.setStatus(request.status());

        if (request.status() == OrderStatus.DELIVERED) {
            paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
                if (payment.getStatus() != PaymentStatus.CAPTURED) {
                    payment.setStatus(PaymentStatus.CAPTURED);
                    payment.setPaidAt(AppDate.now());
                    paymentRepository.save(payment);
                }
            });
        }

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public void delete(UUID id) {
        Order order = findOrThrow(id);
        orderRepository.delete(order);
    }

    private Order findOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found with id: " + id));
    }

    private double calculateDistance(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return 0.0;
        }
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue())) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}

