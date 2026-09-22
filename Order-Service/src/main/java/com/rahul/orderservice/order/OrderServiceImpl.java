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
import com.rahul.orderservice.order.event.OrderCancelledEvent;
import com.rahul.orderservice.order.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService{

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final KafkaTemplate<String, OrderCancelledEvent> kafkaTemplate2;

    @Override
    public OrderResponse placeOrder(String userEmail, PlaceOrderRequest request) {
        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (request.getIdempotencyKey() != null) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                return mapToOrderResponse(existing.get());
            }
        }

        if (cart.getItems().isEmpty()) {
            throw new ResourceNotFoundException("Cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        List<CartItem> processedItems = new ArrayList<>();

        try {
            for (CartItem cartItem : cart.getItems()) {
                productServiceClient.reduceStock(cartItem.getProductId(), cartItem.getQuantity());
                processedItems.add(cartItem);

                OrderItem orderItem = OrderItem.builder()
                        .productId(cartItem.getProductId())
                        .productName(cartItem.getProductName())
                        .price(cartItem.getPrice())
                        .quantity(cartItem.getQuantity())
                        .build();

                orderItems.add(orderItem);
            }
        } catch (Exception e) {
            for (CartItem processed : processedItems) {
                productServiceClient.increaseStock(processed.getProductId(), processed.getQuantity());
            }
            throw new RuntimeException("Order could not be placed, stock has been rolled back");
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
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        orderItems.forEach(item -> item.setOrder(order));

        Order saveOrder = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        kafkaTemplate.send("order-placed-topic",
                OrderPlacedEvent.builder()
                        .orderId(saveOrder.getId())
                        .userEmail(userEmail)
                        .totalAmount(totalAmount)
                        .build());
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

        List<OrderCancelledEvent.CancelledItem> items = order.getItems().stream()
                .map(i -> OrderCancelledEvent.CancelledItem.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .build())
                .collect(Collectors.toList());

        kafkaTemplate2.send("order-cancelled-topic",
                OrderCancelledEvent.builder()
                        .orderId(order.getId())
                        .items(items)
                        .build());

        return mapToOrderResponse(updateOrder);
    }

    @Override
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus current = order.getStatus();

        boolean validTransition =
                (current == OrderStatus.PLACED && newStatus == OrderStatus.CONFIRMED) ||
                        (current == OrderStatus.CONFIRMED && newStatus == OrderStatus.SHIPPED) ||
                        (current == OrderStatus.SHIPPED && newStatus == OrderStatus.DELIVERED);

        if (!validTransition) {
            throw new InvalidOrderStateException("Cannot move from " + current + " to " + newStatus);
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        return mapToOrderResponse(updated);
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
