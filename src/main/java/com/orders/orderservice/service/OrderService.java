package com.orders.orderservice.service;

import com.orders.orderservice.config.OrderMapper;
import com.orders.orderservice.dto.CreateOrderRequest;
import com.orders.orderservice.dto.OrderResponse;
import com.orders.orderservice.dto.UpdateOrderStatusRequest;
import com.orders.orderservice.exception.InvalidOrderStatusException;
import com.orders.orderservice.exception.OrderCancellationException;
import com.orders.orderservice.exception.OrderNotFoundException;
import com.orders.orderservice.model.Order;
import com.orders.orderservice.model.OrderItem;
import com.orders.orderservice.model.OrderStatus;
import com.orders.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    /**
     * Creates a new order with the given items.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerEmail());

        Order order = orderMapper.toEntity(request);
        order.setStatus(OrderStatus.PENDING);

        request.getItems().forEach(itemReq -> {
            OrderItem item = orderMapper.toItemEntity(itemReq);
            order.addItem(item);
        });

        Order saved = orderRepository.save(order);
        log.info("Order created with ID: {}", saved.getId());
        return orderMapper.toResponse(saved);
    }

    /**
     * Retrieves order details by ID.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order with ID: {}", id);
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return orderMapper.toResponse(order);
    }

    /**
     * Lists all orders, optionally filtered by status.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(OrderStatus status) {
        log.info("Listing orders with status filter: {}", status);
        List<Order> orders = (status != null)
                ? orderRepository.findByStatusWithItems(status)
                : orderRepository.findAllWithItems();
        return orderMapper.toResponseList(orders);
    }

    /**
     * Manually updates the status of an order.
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        log.info("Updating order {} status to {}", id, request.getStatus());
        if (request.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusException(
                    "CANCELLED status is not allowed through this endpoint");
        }
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        order.setStatus(request.getStatus());
        Order updated = orderRepository.save(order);
        return orderMapper.toResponse(updated);
    }

    /**
     * Cancels an order. Only PENDING orders can be cancelled.
     */
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        log.info("Cancelling order with ID: {}", id);
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderCancellationException(id, order.getStatus().name());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        log.info("Order {} successfully cancelled", id);
        return orderMapper.toResponse(updated);
    }

    /**
     * Background job: auto-advances PENDING orders to PROCESSING.
     * Called by the scheduler every 5 minutes.
     */
    @Transactional
    public int promoteWaitingOrdersToProcessing() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        log.info("Scheduler: found {} PENDING orders to promote", pendingOrders.size());

        pendingOrders.forEach(order -> order.setStatus(OrderStatus.PROCESSING));
        orderRepository.saveAll(pendingOrders);

        return pendingOrders.size();
    }
}
