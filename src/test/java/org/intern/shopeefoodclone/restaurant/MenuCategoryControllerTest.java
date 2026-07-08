package org.intern.shopeefoodclone.restaurant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryController;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryCreateRequest;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryResponse;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryService;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MenuCategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MenuCategoryService menuCategoryService;

    @InjectMocks
    private MenuCategoryController menuCategoryController;

    private ObjectMapper objectMapper;
    private UUID restaurantId;
    private UUID categoryId;
    private MenuCategoryCreateRequest createRequest;
    private MenuCategoryResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(menuCategoryController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionalHandler())
                .build();

        restaurantId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        createRequest = new MenuCategoryCreateRequest("Pizzas", 1);
        response = new MenuCategoryResponse(categoryId, restaurantId, "Pizzas", 1, Collections.emptyList());
    }

    @Test
    void testCreateCategory_Success() throws Exception {
        when(menuCategoryService.create(eq(restaurantId), any(MenuCategoryCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/restaurants/" + restaurantId + "/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Menu category created successfully"))
                .andExpect(jsonPath("$.data.name").value("Pizzas"));

        verify(menuCategoryService).create(eq(restaurantId), any(MenuCategoryCreateRequest.class));
    }

    @Test
    void testGetByRestaurantId_Success() throws Exception {
        PageResponse<MenuCategoryResponse> pageResponse = PageResponse.<MenuCategoryResponse>builder()
                .content(List.of(response))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .build();
        when(menuCategoryService.findByRestaurantId(eq(restaurantId), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/restaurants/" + restaurantId + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].name").value("Pizzas"));

        verify(menuCategoryService).findByRestaurantId(eq(restaurantId), any(), any(Pageable.class));
    }

    @Test
    void testDeleteCategory_Success() throws Exception {
        mockMvc.perform(delete("/api/categories/" + categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Menu category deleted successfully"));

        verify(menuCategoryService).delete(categoryId);
    }
}
