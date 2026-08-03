package org.intern.shopeefoodclone.order;

import org.intern.shopeefoodclone.cart.CartService;
import org.intern.shopeefoodclone.infras.cache.CacheService;
import org.intern.shopeefoodclone.infras.messaging.KafkaEventPublisher;
import org.intern.shopeefoodclone.cart.Cart;
import org.intern.shopeefoodclone.cart.CartItem;
import org.intern.shopeefoodclone.order.dto.OrderCreateRequest;
import org.intern.shopeefoodclone.order.dto.OrderResponse;
import org.intern.shopeefoodclone.order.enums.DeliveryMethod;
import org.intern.shopeefoodclone.payment.PaymenMethodType;
import org.intern.shopeefoodclone.payment.PaymentMethodRequest;
import org.intern.shopeefoodclone.payment.PaymentRepository;
import org.intern.shopeefoodclone.restaurant.Restaurant;
import org.intern.shopeefoodclone.restaurant.RestaurantRepository;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategory;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItem;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemRepository;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.intern.shopeefoodclone.user.User;
import org.intern.shopeefoodclone.user.UserRepository;
import org.intern.shopeefoodclone.user.address.Address;
import org.intern.shopeefoodclone.user.address.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CartService cartService;

    @Mock
    private CacheService cacheService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private UUID restaurantId;
    private UUID addressId;
    private UUID menuItemId;
    private User user;
    private Restaurant restaurant;
    private Address address;
    private MenuItem menuItem;
    private OrderCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        menuItemId = UUID.randomUUID();

        user = User.builder().id(userId).email("customer@test.com").build();
        address = Address.builder().id(addressId).user(user).line1("123 Street").city("HCMC").build();

        restaurant = Restaurant.builder()
                .id(restaurantId)
                .name("Test Food")
                .isOpen(true)
                .build();

        MenuCategory category = MenuCategory.builder()
                .restaurant(restaurant)
                .name("Main")
                .build();

        menuItem = MenuItem.builder()
                .id(menuItemId)
                .category(category)
                .name("Pizza")
                .price(BigDecimal.valueOf(100000.00))
                .isAvailable(true)
                .build();

        PaymentMethodRequest paymentMethodRequest = new PaymentMethodRequest(PaymenMethodType.COD, null, null);
        createRequest = new OrderCreateRequest(restaurantId, addressId, DeliveryMethod.DELIVERY, "Instructions", paymentMethodRequest);

        // Setup security context mock for SecurityUtils
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId.toString());
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testCreateOrder_Success() {
        when(cacheService.hasKey(anyString())).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        CartItem cartItem = CartItem.builder()
                .itemId(menuItemId.toString())
                .itemName("Pizza")
                .quantity(2)
                .price(BigDecimal.valueOf(100000.00))
                .build();
        Cart cart = Cart.builder()
                .cartId(userId.toString())
                .restaurantId(restaurantId.toString())
                .items(List.of(cartItem))
                .build();
        when(cartService.getCart(userId.toString())).thenReturn(cart);

        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));

        Order order = Order.builder().id(UUID.randomUUID()).user(user).restaurant(restaurant).build();
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(order.getId())
                .userId(userId)
                .restaurantId(restaurantId)
                .build();
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

        OrderResponse response = orderService.create(createRequest);

        assertNotNull(response);
        assertEquals(order.getId(), response.id());
        verify(orderRepository).save(any(Order.class));
        verify(paymentRepository).save(any());
        verify(kafkaEventPublisher).publishOrderPlaced(any());
    }

    @Test
    void testCreateOrder_RestaurantClosed() {
        restaurant.setIsOpen(false);
        when(cacheService.hasKey(anyString())).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        AppException ex = assertThrows(AppException.class, () -> orderService.create(createRequest));
        assertEquals(ErrorCode.RESTAURANT_CLOSED, ex.getErrorCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_ItemOutOfStock() {
        menuItem.setIsAvailable(false);
        when(cacheService.hasKey(anyString())).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        CartItem cartItem = CartItem.builder()
                .itemId(menuItemId.toString())
                .itemName("Pizza")
                .quantity(2)
                .price(BigDecimal.valueOf(100000.00))
                .build();
        Cart cart = Cart.builder()
                .cartId(userId.toString())
                .restaurantId(restaurantId.toString())
                .items(List.of(cartItem))
                .build();
        when(cartService.getCart(userId.toString())).thenReturn(cart);

        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));

        AppException ex = assertThrows(AppException.class, () -> orderService.create(createRequest));
        assertEquals(ErrorCode.OUT_OF_STOCK, ex.getErrorCode());
        verify(orderRepository, never()).save(any());
    }
}
