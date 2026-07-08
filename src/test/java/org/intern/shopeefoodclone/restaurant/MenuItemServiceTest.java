package org.intern.shopeefoodclone.restaurant;

import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategory;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryRepository;
import org.intern.shopeefoodclone.restaurant.menu.item.*;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private MenuCategoryRepository menuCategoryRepository;

    @Mock
    private MenuItemMapper menuItemMapper;

    @InjectMocks
    private MenuItemService menuItemService;

    private UUID categoryId;
    private UUID itemId;
    private MenuCategory category;
    private MenuItem item;
    private MenuItemCreateRequest createRequest;
    private MenuItemResponse response;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        category = MenuCategory.builder()
                .id(categoryId)
                .name("Beverages")
                .build();

        item = MenuItem.builder()
                .id(itemId)
                .category(category)
                .name("Iced Coffee")
                .price(BigDecimal.valueOf(3.50))
                .isAvailable(true)
                .build();

        createRequest = new MenuItemCreateRequest("Iced Coffee", "Cold coffee", BigDecimal.valueOf(3.50), "url", true);
        response = new MenuItemResponse(itemId, categoryId, "Iced Coffee", "Cold coffee", BigDecimal.valueOf(3.50), "url", true);
    }

    @Test
    void testCreate_Success() {
        when(menuCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(menuItemRepository.existsByCategoryIdAndName(categoryId, "Iced Coffee")).thenReturn(false);
        when(menuItemMapper.toEntity(createRequest)).thenReturn(item);
        when(menuItemRepository.save(item)).thenReturn(item);
        when(menuItemMapper.toResponse(item)).thenReturn(response);

        MenuItemResponse res = menuItemService.create(categoryId, createRequest);

        assertNotNull(res);
        assertEquals("Iced Coffee", res.name());
        verify(menuItemRepository).save(item);
    }

    @Test
    void testCreate_CategoryNotFound() {
        when(menuCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> menuItemService.create(categoryId, createRequest));
        assertEquals(ErrorCode.MENU_CATEGORY_NOT_FOUND, ex.getErrorCode());
        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void testCreate_DuplicateName() {
        when(menuCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(menuItemRepository.existsByCategoryIdAndName(categoryId, "Iced Coffee")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> menuItemService.create(categoryId, createRequest));
        assertEquals(ErrorCode.MENU_ITEM_ALREADY_EXISTS, ex.getErrorCode());
        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void testFindByCategoryId_Success() {
        when(menuCategoryRepository.existsById(categoryId)).thenReturn(true);
        when(menuItemRepository.findByCategoryId(categoryId)).thenReturn(List.of(item));
        when(menuItemMapper.toResponseList(List.of(item))).thenReturn(List.of(response));

        List<MenuItemResponse> list = menuItemService.findByCategoryId(categoryId);

        assertEquals(1, list.size());
        assertEquals("Iced Coffee", list.get(0).name());
    }

    @Test
    void testFindByCategoryId_CategoryNotFound() {
        when(menuCategoryRepository.existsById(categoryId)).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> menuItemService.findByCategoryId(categoryId));
        assertEquals(ErrorCode.MENU_CATEGORY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void testDelete_Success() {
        when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        menuItemService.delete(itemId);

        verify(menuItemRepository).delete(item);
    }
}
