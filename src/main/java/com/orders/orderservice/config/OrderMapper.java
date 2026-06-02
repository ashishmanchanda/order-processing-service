package com.orders.orderservice.config;

import com.orders.orderservice.dto.CreateOrderRequest;
import com.orders.orderservice.dto.OrderItemRequest;
import com.orders.orderservice.dto.OrderItemResponse;
import com.orders.orderservice.dto.OrderResponse;
import com.orders.orderservice.model.Order;
import com.orders.orderservice.model.OrderItem;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "items", ignore = true)
    Order toEntity(CreateOrderRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    OrderItem toItemEntity(OrderItemRequest request);

    @Mapping(target = "totalPrice", expression = "java(calculateItemTotal(item))")
    OrderItemResponse toItemResponse(OrderItem item);

    @Mapping(target = "totalAmount", expression = "java(calculateOrderTotal(order))")
    OrderResponse toResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);

    default BigDecimal calculateItemTotal(OrderItem item) {
        if (item.getUnitPrice() == null || item.getQuantity() == null) return BigDecimal.ZERO;
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    default BigDecimal calculateOrderTotal(Order order) {
        if (order.getItems() == null) return BigDecimal.ZERO;
        return order.getItems().stream()
                .map(this::calculateItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
