package com.orders.orderservice.controller;

import com.orders.orderservice.dto.ApiResponse;
import com.orders.orderservice.dto.CreateOrderRequest;
import com.orders.orderservice.dto.OrderResponse;
import com.orders.orderservice.dto.UpdateOrderStatusRequest;
import com.orders.orderservice.model.OrderStatus;
import com.orders.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order Processing APIs")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Order created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Order fetched successfully"));
    }

    @GetMapping
    @Operation(summary = "List all orders, optionally filtered by status")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @Parameter(description = "Filter by order status")
            @RequestParam(required = false) OrderStatus status) {
        List<OrderResponse> orders = orderService.getAllOrders(status);
        return ResponseEntity.ok(ApiResponse.success(orders,
                "Fetched " + orders.size() + " order(s)"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse response = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Order status updated"));
    }

    @DeleteMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order (only if PENDING)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Order cancelled successfully"));
    }
}
