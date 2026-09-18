package com.rahul.orderservice.order;

import com.rahul.orderservice.order.dto.OrderResponse;
import com.rahul.orderservice.order.dto.PlaceOrderRequest;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse placeOrder(String userEmail, PlaceOrderRequest request);
    List<OrderResponse> getMyOrder(String userEmail);
    OrderResponse getOrderById(UUID id);
    OrderResponse cancelOrder(String userEmail, UUID orderId);

}
