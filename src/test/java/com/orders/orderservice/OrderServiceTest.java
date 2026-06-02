package com.orders.orderservice;

import com.orders.orderservice.config.OrderMapper;
import com.orders.orderservice.dto.CreateOrderRequest;
import com.orders.orderservice.dto.OrderItemRequest;
import com.orders.orderservice.dto.OrderResponse;
import com.orders.orderservice.dto.UpdateOrderStatusRequest;
import com.orders.orderservice.exception.OrderCancellationException;
import com.orders.orderservice.exception.OrderNotFoundException;
import com.orders.orderservice.model.Order;
import com.orders.orderservice.model.OrderItem;
import com.orders.orderservice.model.OrderStatus;
import com.orders.orderservice.repository.OrderRepository;
import com.orders.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;
    private OrderResponse sampleOrderResponse;

    @BeforeEach
    void setUp() {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productName("Laptop")
                .productCode("LAP-001")
                .quantity(2)
                .unitPrice(new BigDecimal("999.99"))
                .build();

        sampleOrder = Order.builder()
                .id(1L)
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>(List.of(item)))
                .build();

        sampleOrderResponse = OrderResponse.builder()
                .id(1L)
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1999.98"))
                .build();
    }

    @Test
    @DisplayName("Should create order successfully")
    void createOrder_success() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .items(List.of(OrderItemRequest.builder()
                        .productName("Laptop")
                        .productCode("LAP-001")
                        .quantity(2)
                        .unitPrice(new BigDecimal("999.99"))
                        .build()))
                .build();

        when(orderMapper.toEntity(any())).thenReturn(sampleOrder);
        when(orderMapper.toItemEntity(any())).thenReturn(sampleOrder.getItems().get(0));
        when(orderRepository.save(any())).thenReturn(sampleOrder);
        when(orderMapper.toResponse(any())).thenReturn(sampleOrderResponse);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.getCustomerEmail()).isEqualTo("john@example.com");
        verify(orderRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order not found")
    void getOrderById_notFound() {
        when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Should cancel a PENDING order")
    void cancelOrder_pendingOrder_success() {
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenReturn(sampleOrder);
        when(orderMapper.toResponse(any())).thenReturn(
                sampleOrderResponse.toBuilder().status(OrderStatus.CANCELLED).build());

        OrderResponse result = orderService.cancelOrder(1L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should throw when cancelling a non-PENDING order")
    void cancelOrder_nonPending_throwsException() {
        sampleOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(OrderCancellationException.class)
                .hasMessageContaining("SHIPPED");
    }

    @Test
    @DisplayName("Should promote PENDING orders to PROCESSING")
    void promoteWaitingOrders_success() {
        List<Order> pendingOrders = List.of(sampleOrder);
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(pendingOrders);
        when(orderRepository.saveAll(any())).thenReturn(pendingOrders);

        int count = orderService.promoteWaitingOrdersToProcessing();

        assertThat(count).isEqualTo(1);
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    @DisplayName("Should update order status")
    void updateOrderStatus_success() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.SHIPPED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenReturn(sampleOrder);
        when(orderMapper.toResponse(any())).thenReturn(
                sampleOrderResponse.toBuilder().status(OrderStatus.SHIPPED).build());

        OrderResponse result = orderService.updateOrderStatus(1L, request);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }
}
