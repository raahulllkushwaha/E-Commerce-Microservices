package com.rahul.orderservice.order;

import com.rahul.orderservice.cart.Cart;
import com.rahul.orderservice.cart.CartItem;
import com.rahul.orderservice.cart.CartRepository;
import com.rahul.orderservice.client.ProductServiceClient;
import com.rahul.orderservice.common.exception.InvalidCredentialsException;
import com.rahul.orderservice.common.exception.InvalidOrderStateException;
import com.rahul.orderservice.common.exception.ResourceNotFoundException;
import com.rahul.orderservice.order.dto.OrderItemResponse;
import com.rahul.orderservice.order.dto.OrderResponse;
import com.rahul.orderservice.order.dto.PlaceOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService{

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;

    @Override
    public OrderResponse placeOrder(String userEmail, PlaceOrderRequest request) {
        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new ResourceNotFoundException("Cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            productServiceClient.reduceStock(cartItem.getProductId(), cartItem.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .price(cartItem.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();

            orderItems.add(orderItem);
        }
        BigDecimal totalAmount = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userEmail(userEmail)
                .items(orderItems)
                .totalAmount(totalAmount)
                .status(OrderStatus.PLACED)
                .addressId(request.getAddressId())
                .build();

        orderItems.forEach(item -> item.setOrder(order));

        Order saveOrder = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return mapToOrderResponse(saveOrder);
    }

    @Override
    public List<OrderResponse> getMyOrders(String userEmail) {
        List<Order> orders = orderRepository.findByUserEmail(userEmail);

        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order by this ID not found"));

        return mapToOrderResponse(order);
    }

    @Override
    public OrderResponse cancelOrder(String userEmail, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if(!order.getUserEmail().equals(userEmail)) {
            throw new InvalidCredentialsException("You are not allowed to cancel this order");
        }

        if(order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED){
            throw new InvalidOrderStateException("Cannot cancel a shipped or delivered order");
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order updateOrder = orderRepository.save(order);

        return mapToOrderResponse(updateOrder);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userEmail(order.getUserEmail())
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .addressId(order.getAddressId())
                .build();
    }

}
