package org.intern.shopeefoodclone.restaurant;

import org.intern.shopeefoodclone.restaurant.menu.category.*;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemMapper;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemRepository;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuCategoryServiceTest {

    @Mock
    private MenuCategoryRepository menuCategoryRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private MenuCategoryMapper menuCategoryMapper;

    @Mock
    private MenuItemMapper menuItemMapper;

    @InjectMocks
    private MenuCategoryService menuCategoryService;

    private UUID restaurantId;
    private UUID categoryId;
    private Restaurant restaurant;
    private MenuCategory category;
    private MenuCategoryCreateRequest createRequest;
    private MenuCategoryResponse response;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        restaurant = Restaurant.builder()
                .id(restaurantId)
                .name("Pizza Palace")
                .build();

        category = MenuCategory.builder()
                .id(categoryId)
                .restaurant(restaurant)
                .name("Pizzas")
                .sortOrder(1)
                .build();

        createRequest = new MenuCategoryCreateRequest("Pizzas", 1);
        response = new MenuCategoryResponse(categoryId, restaurantId, "Pizzas", 1, Collections.emptyList());
    }

    @Test
    void testCreate_Success() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuCategoryRepository.existsByRestaurantIdAndName(restaurantId, "Pizzas")).thenReturn(false);
        when(menuCategoryMapper.toEntity(createRequest)).thenReturn(category);
        when(menuCategoryRepository.save(category)).thenReturn(category);
        when(menuCategoryMapper.toResponse(category)).thenReturn(response);

        MenuCategoryResponse res = menuCategoryService.create(restaurantId, createRequest);

        assertNotNull(res);
        assertEquals("Pizzas", res.name());
        verify(menuCategoryRepository).save(category);
    }

    @Test
    void testCreate_RestaurantNotFound() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> menuCategoryService.create(restaurantId, createRequest));
        assertEquals(ErrorCode.RESTAURANT_NOT_FOUND, ex.getErrorCode());
        verify(menuCategoryRepository, never()).save(any());
    }

    @Test
    void testCreate_DuplicateName() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuCategoryRepository.existsByRestaurantIdAndName(restaurantId, "Pizzas")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> menuCategoryService.create(restaurantId, createRequest));
        assertEquals(ErrorCode.MENU_CATEGORY_ALREADY_EXISTS, ex.getErrorCode());
        verify(menuCategoryRepository, never()).save(any());
    }

    @Test
    void testFindByRestaurantId_Success() {
        when(restaurantRepository.existsById(restaurantId)).thenReturn(true);
        when(menuCategoryRepository.findByRestaurantIdOrderBySortOrderAsc(restaurantId)).thenReturn(List.of(category));
        when(menuItemRepository.findByCategoryId(categoryId)).thenReturn(Collections.emptyList());
        when(menuItemMapper.toResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(menuCategoryMapper.toResponseWithItems(category, Collections.emptyList())).thenReturn(response);

        List<MenuCategoryResponse> list = menuCategoryService.findByRestaurantId(restaurantId);

        assertEquals(1, list.size());
        assertEquals("Pizzas", list.get(0).name());
    }

    @Test
    void testDelete_Success() {
        when(menuCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(menuItemRepository.findByCategoryId(categoryId)).thenReturn(Collections.emptyList());

        menuCategoryService.delete(categoryId);

        verify(menuCategoryRepository).delete(category);
    }
}
