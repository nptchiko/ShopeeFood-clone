package org.intern.shopeefoodclone.restaurant;

import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryRepository;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryService;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.intern.shopeefoodclone.user.address.Address;
import org.intern.shopeefoodclone.user.address.AddressRepository;
import org.intern.shopeefoodclone.user.User;
import org.intern.shopeefoodclone.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private MenuCategoryRepository menuCategoryRepository;

    @Mock
    private RestaurantMapper restaurantMapper;

    @Mock
    private MenuCategoryService menuCategoryService;

    @InjectMocks
    private RestaurantService restaurantService;

    private UUID restaurantId;
    private UUID ownerId;
    private UUID addressId;
    private User owner;
    private Address address;
    private Restaurant restaurant;
    private RestaurantCreateRequest createRequest;
    private RestaurantResponse response;
    private RestaurantDetailResponse detailResponse;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        owner = User.builder().id(ownerId).email("owner@test.com").build();
        address = Address.builder().id(addressId).line1("123 Street").build();

        restaurant = Restaurant.builder()
                .id(restaurantId)
                .owner(owner)
                .name("Burger King")
                .address(address)
                .rating(BigDecimal.ZERO)
                .isOpen(true)
                .build();

        createRequest = new RestaurantCreateRequest(ownerId, "Burger King", "Fast food", "logo", "banner", true);
        response = new RestaurantResponse(restaurantId, ownerId, "Burger King", addressId, "Fast food", "logo", "banner", BigDecimal.ZERO, true, OffsetDateTime.now(), OffsetDateTime.now());
        detailResponse = new RestaurantDetailResponse(restaurantId, ownerId, "Burger King", addressId, "Fast food", "logo", "banner", BigDecimal.ZERO, true, Collections.emptyList());
    }

    @Test
    void testCreate_Success() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(restaurantMapper.toEntity(createRequest)).thenReturn(restaurant);
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);
        when(restaurantMapper.toResponse(restaurant)).thenReturn(response);

        RestaurantResponse res = restaurantService.create(createRequest);

        assertNotNull(res);
        assertEquals("Burger King", res.name());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void testCreate_OwnerNotFound() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> restaurantService.create(createRequest));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void testCreate_AddressNotFound() {
        // RestaurantCreateRequest has no addressId — address is linked via update(),
        // not at creation time. Verify create() never consults the address repository.
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(restaurantMapper.toEntity(createRequest)).thenReturn(restaurant);
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);
        when(restaurantMapper.toResponse(restaurant)).thenReturn(response);

        RestaurantResponse res = restaurantService.create(createRequest);

        assertNotNull(res);
        verify(addressRepository, never()).findById(any());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void testGetById_Success() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuCategoryService.findByRestaurantId(restaurantId)).thenReturn(Collections.emptyList());
        when(restaurantMapper.toDetailResponse(restaurant, Collections.emptyList())).thenReturn(detailResponse);

        RestaurantDetailResponse res = restaurantService.getById(restaurantId);

        assertNotNull(res);
        assertEquals("Burger King", res.name());
    }

    @Test
    void testGetById_NotFound() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> restaurantService.getById(restaurantId));
        assertEquals(ErrorCode.RESTAURANT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void testDelete_Success() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuCategoryRepository.findByRestaurantIdOrderBySortOrderAsc(restaurantId)).thenReturn(Collections.emptyList());

        restaurantService.delete(restaurantId);

        verify(restaurantRepository).delete(restaurant);
    }
}
