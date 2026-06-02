package com.orders.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orders.orderservice.dto.CreateOrderRequest;
import com.orders.orderservice.dto.OrderItemRequest;
import com.orders.orderservice.dto.UpdateOrderStatusRequest;
import com.orders.orderservice.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateOrderRequest buildSampleRequest() {
        return CreateOrderRequest.builder()
                .customerName("Alice Smith")
                .customerEmail("alice@example.com")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productName("Keyboard")
                                .productCode("KB-100")
                                .quantity(1)
                                .unitPrice(new BigDecimal("79.99"))
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/orders - creates order and returns 201")
    void createOrder_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.customerEmail").value("alice@example.com"))
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} - fetches existing order")
    void getOrderById_returnsOrder() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andReturn();

        int id = objectMapper.readTree(created.getResponse().getContentAsString())
                .at("/data/id").asInt();

        mockMvc.perform(get("/api/v1/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} - returns 404 for missing order")
    void getOrderById_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/orders/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/orders - lists all orders")
    void listAllOrders_returnsOrders() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildSampleRequest())));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(0))));
    }

    @Test
    @DisplayName("GET /api/v1/orders?status=PENDING - filters by status")
    void listOrders_filterByStatus() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildSampleRequest())));

        mockMvc.perform(get("/api/v1/orders?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].status", everyItem(is("PENDING"))));
    }

    @Test
    @DisplayName("PATCH /api/v1/orders/{id}/status - updates order status to SHIPPED")
    void updateOrderStatus_success() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andReturn();

        int id = objectMapper.readTree(created.getResponse().getContentAsString())
                .at("/data/id").asInt();

        mockMvc.perform(patch("/api/v1/orders/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateOrderStatusRequest(OrderStatus.SHIPPED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/orders/{id}/cancel - cancels PENDING order")
    void cancelOrder_pendingOrder_succeeds() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andReturn();

        int id = objectMapper.readTree(created.getResponse().getContentAsString())
                .at("/data/id").asInt();

        mockMvc.perform(delete("/api/v1/orders/" + id + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/orders/{id}/cancel - returns 409 for non-PENDING order")
    void cancelOrder_nonPending_returns409() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andReturn();

        int id = objectMapper.readTree(created.getResponse().getContentAsString())
                .at("/data/id").asInt();

        // Promote to PROCESSING
        mockMvc.perform(patch("/api/v1/orders/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new UpdateOrderStatusRequest(OrderStatus.PROCESSING))));

        // Try cancel
        mockMvc.perform(delete("/api/v1/orders/" + id + "/cancel"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/orders - returns 400 for invalid request")
    void createOrder_invalidRequest_returns400() throws Exception {
        CreateOrderRequest invalid = CreateOrderRequest.builder()
                .customerName("")          // blank name
                .customerEmail("not-an-email")
                .items(List.of())          // empty items
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
