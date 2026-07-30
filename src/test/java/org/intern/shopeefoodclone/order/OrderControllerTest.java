package org.intern.shopeefoodclone.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intern.shopeefoodclone.order.dto.OrderResponse;
import org.intern.shopeefoodclone.order.enums.DeliveryMethod;
import org.intern.shopeefoodclone.order.enums.OrderStatus;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private ObjectMapper objectMapper;
    private UUID orderId;
    private OrderResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionalHandler())
                .build();

        orderId = UUID.randomUUID();
        response = OrderResponse.builder()
                .id(orderId)
                .userId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .deliveryMethod(DeliveryMethod.DELIVERY)
                .status(OrderStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void testGetOrderById_Success() throws Exception {
        when(orderService.getById(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(orderId.toString()));

        verify(orderService).getById(orderId);
    }

    @Test
    void testGetAllOrders_Success() throws Exception {
        PageResponse<OrderResponse> pageResponse = PageResponse.<OrderResponse>builder()
                .content(List.of(response))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .build();
        when(orderService.findAll(any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(orderId.toString()));

        verify(orderService).findAll(any(), any(Pageable.class));
    }

    @Test
    void testDeleteOrder_Success() throws Exception {
        mockMvc.perform(delete("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Order deleted successfully"));

        verify(orderService).delete(orderId);
    }
}
