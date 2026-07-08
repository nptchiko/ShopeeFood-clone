package org.intern.shopeefoodclone.restaurant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemController;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemCreateRequest;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemResponse;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemService;
import org.intern.shopeefoodclone.shared.api.PageResponse;
import org.intern.shopeefoodclone.shared.exception.GlobalExceptionalHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MenuItemControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MenuItemService menuItemService;

    @InjectMocks
    private MenuItemController menuItemController;

    private ObjectMapper objectMapper;
    private UUID categoryId;
    private UUID itemId;
    private MenuItemCreateRequest createRequest;
    private MenuItemResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(menuItemController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionalHandler())
                .build();

        categoryId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        createRequest = new MenuItemCreateRequest("Iced Coffee", "Cold coffee", BigDecimal.valueOf(3.50), "url", true);
        response = new MenuItemResponse(itemId, categoryId, "Iced Coffee", "Cold coffee", BigDecimal.valueOf(3.50), "url", true);
    }

    @Test
    void testCreateItem_Success() throws Exception {
        when(menuItemService.create(eq(categoryId), any(MenuItemCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories/" + categoryId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Menu item created successfully"))
                .andExpect(jsonPath("$.data.name").value("Iced Coffee"));

        verify(menuItemService).create(eq(categoryId), any(MenuItemCreateRequest.class));
    }

    @Test
    void testGetByCategoryId_Success() throws Exception {
        PageResponse<MenuItemResponse> pageResponse = PageResponse.<MenuItemResponse>builder()
                .content(List.of(response))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .build();
        when(menuItemService.findByCategoryId(eq(categoryId), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/categories/" + categoryId + "/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].name").value("Iced Coffee"));

        verify(menuItemService).findByCategoryId(eq(categoryId), any(), any(Pageable.class));
    }

    @Test
    void testDeleteItem_Success() throws Exception {
        mockMvc.perform(delete("/api/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Menu item deleted successfully"));

        verify(menuItemService).delete(itemId);
    }
}
