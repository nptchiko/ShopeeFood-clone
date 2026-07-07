package org.intern.shopeefoodclone.restaurant;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.OffsetDateTime;
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
class RestaurantControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RestaurantService restaurantService;

    @InjectMocks
    private RestaurantController restaurantController;

    private ObjectMapper objectMapper;
    private UUID restaurantId;
    private UUID ownerId;
    private UUID addressId;
    private RestaurantCreateRequest createRequest;
    private RestaurantResponse response;
    private RestaurantDetailResponse detailResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(restaurantController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionalHandler())
                .build();

        restaurantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        createRequest = new RestaurantCreateRequest(ownerId, "Burger King", addressId, "Fast food", "logo", "banner", true);
        response = new RestaurantResponse(restaurantId, ownerId, "Burger King", addressId, "Fast food", "logo", "banner", BigDecimal.ZERO, true, OffsetDateTime.now(), OffsetDateTime.now());
        detailResponse = new RestaurantDetailResponse(restaurantId, ownerId, "Burger King", addressId, "Fast food", "logo", "banner", BigDecimal.ZERO, true, Collections.emptyList(), OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void testCreateRestaurant_Success() throws Exception {
        when(restaurantService.create(any(RestaurantCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Restaurant created successfully"))
                .andExpect(jsonPath("$.data.name").value("Burger King"));

        verify(restaurantService).create(any(RestaurantCreateRequest.class));
    }

    @Test
    void testGetAllRestaurants_Success() throws Exception {
        PageResponse<RestaurantResponse> pageResponse = PageResponse.<RestaurantResponse>builder()
                .content(List.of(response))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .build();
        when(restaurantService.findAll(any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].name").value("Burger King"));

        verify(restaurantService).findAll(any(), any(Pageable.class));
    }

    @Test
    void testGetRestaurantById_Success() throws Exception {
        when(restaurantService.getById(restaurantId)).thenReturn(detailResponse);

        mockMvc.perform(get("/api/restaurants/" + restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("Burger King"));

        verify(restaurantService).getById(restaurantId);
    }

    @Test
    void testDeleteRestaurant_Success() throws Exception {
        mockMvc.perform(delete("/api/restaurants/" + restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Restaurant deleted successfully"));

        verify(restaurantService).delete(restaurantId);
    }
}
