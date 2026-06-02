package com.orders.orderservice.exception;

public class OrderCancellationException extends RuntimeException {
    public OrderCancellationException(Long id, String status) {
        super("Order ID " + id + " cannot be cancelled because it is in status: " + status);
    }
}
