package com.rahul.orderservice.order;

import com.rahul.orderservice.cart.Cart;
import com.rahul.orderservice.cart.CartItem;
import com.rahul.orderservice.cart.CartRepository;
import com.rahul.orderservice.client.ProductServiceClient;
import com.rahul.orderservice.common.exception.InvalidCredentialsException;
import com.rahul.orderservice.common.exception.InvalidOrderStateException;
import com.rahul.orderservice.common.exception.ResourceNotFoundException;
import com.rahul.orderservice.order.dto.OrderResponse;
import com.rahul.orderservice.order.dto.PlaceOrderRequest;
import com.rahul.orderservice.order.event.OrderCancelledEvent;
import com.rahul.orderservice.order.event.OrderPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Mock
    private KafkaTemplate<String, OrderCancelledEvent> kafkaTemplate2;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                cartRepository,
                productServiceClient,
                kafkaTemplate,
                kafkaTemplate2
        );
    }

    @Test
    void shouldPlaceOrderSuccessfully() {

        String email = "rahul@gmail.com";

        UUID productId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        CartItem item = CartItem.builder()
                .productId(productId)
                .productName("Laptop")
                .price(new BigDecimal("50000"))
                .quantity(1)
                .build();

        Cart cart = Cart.builder()
                .userEmail(email)
                .items(new ArrayList<>(List.of(item)))
                .build();

        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .addressId(addressId)
                .idempotencyKey("ORDER-123")
                .build();

        when(cartRepository.findByUserEmail(email))
                .thenReturn(Optional.of(cart));

        doNothing()
                .when(productServiceClient)
                .reduceStock(productId, 1);

        Order savedOrder = Order.builder()
                .id(UUID.randomUUID())
                .userEmail(email)
                .items(new ArrayList<>())
                .totalAmount(new BigDecimal("50000"))
                .status(OrderStatus.PLACED)
                .addressId(addressId)
                .idempotencyKey("ORDER-123")
                .build();

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        OrderResponse response =
                orderService.placeOrder(email, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus())
                .isEqualTo(OrderStatus.PLACED);

        verify(productServiceClient)
                .reduceStock(productId, 1);

        verify(orderRepository)
                .save(any(Order.class));

        verify(cartRepository)
                .save(cart);

        verify(kafkaTemplate)
                .send(
                        eq("order-placed-topic"),
                        any(OrderPlacedEvent.class)
                );
    }

    @Test
    void shouldRejectOrderWhenCartIsEmpty() {

        String email = "rahul@gmail.com";

        Cart cart = Cart.builder()
                .userEmail(email)
                .items(new ArrayList<>())
                .build();

        when(cartRepository.findByUserEmail(email))
                .thenReturn(Optional.of(cart));

        PlaceOrderRequest request =
                PlaceOrderRequest.builder()
                        .addressId(UUID.randomUUID())
                        .build();

        assertThatThrownBy(() ->
                orderService.placeOrder(email, request)
        )
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productServiceClient, never())
                .reduceStock(any(), anyInt());
    }

    @Test
    void shouldReturnExistingOrderForDuplicateIdempotencyKey() {

        String email = "rahul@gmail.com";
        String key = "ORDER-123";

        Cart cart = Cart.builder()
                .userEmail(email)
                .items(new ArrayList<>())
                .build();

        Order existingOrder = Order.builder()
                .id(UUID.randomUUID())
                .userEmail(email)
                .status(OrderStatus.PLACED)
                .totalAmount(new BigDecimal("50000"))
                .addressId(UUID.randomUUID())
                .idempotencyKey(key)
                .items(new ArrayList<>())
                .build();

        when(cartRepository.findByUserEmail(email))
                .thenReturn(Optional.of(cart));

        when(orderRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.of(existingOrder));

        PlaceOrderRequest request =
                PlaceOrderRequest.builder()
                        .addressId(existingOrder.getAddressId())
                        .idempotencyKey(key)
                        .build();

        OrderResponse response =
                orderService.placeOrder(email, request);

        assertThat(response.getId())
                .isEqualTo(existingOrder.getId());

        verify(productServiceClient, never())
                .reduceStock(any(), anyInt());

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldRollbackProcessedStockWhenOrderFails() {

        String email = "rahul@gmail.com";

        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        UUID productC = UUID.randomUUID();

        CartItem itemA = CartItem.builder()
                .productId(productA)
                .productName("A")
                .price(new BigDecimal("100"))
                .quantity(1)
                .build();

        CartItem itemB = CartItem.builder()
                .productId(productB)
                .productName("B")
                .price(new BigDecimal("200"))
                .quantity(1)
                .build();

        CartItem itemC = CartItem.builder()
                .productId(productC)
                .productName("C")
                .price(new BigDecimal("300"))
                .quantity(1)
                .build();

        Cart cart = Cart.builder()
                .userEmail(email)
                .items(new ArrayList<>(
                        List.of(itemA, itemB, itemC)
                ))
                .build();

        when(cartRepository.findByUserEmail(email))
                .thenReturn(Optional.of(cart));

        doNothing()
                .when(productServiceClient)
                .reduceStock(productA, 1);

        doNothing()
                .when(productServiceClient)
                .reduceStock(productB, 1);

        doThrow(new RuntimeException("Stock unavailable"))
                .when(productServiceClient)
                .reduceStock(productC, 1);

        PlaceOrderRequest request =
                PlaceOrderRequest.builder()
                        .addressId(UUID.randomUUID())
                        .build();

        assertThatThrownBy(() ->
                orderService.placeOrder(email, request)
        )
                .isInstanceOf(RuntimeException.class);

        verify(productServiceClient)
                .increaseStock(productA, 1);

        verify(productServiceClient)
                .increaseStock(productB, 1);

        verify(productServiceClient, never())
                .increaseStock(productC, 1);

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldCancelOrderSuccessfully() {

        String email = "rahul@gmail.com";

        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItem item = OrderItem.builder()
                .productId(productId)
                .productName("Laptop")
                .price(new BigDecimal("50000"))
                .quantity(1)
                .build();

        Order order = Order.builder()
                .id(orderId)
                .userEmail(email)
                .items(new ArrayList<>(List.of(item)))
                .status(OrderStatus.PLACED)
                .totalAmount(new BigDecimal("50000"))
                .addressId(UUID.randomUUID())
                .build();

        item.setOrder(order);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        OrderResponse response =
                orderService.cancelOrder(email, orderId);

        assertThat(response.getStatus())
                .isEqualTo(OrderStatus.CANCELLED);

        verify(orderRepository)
                .save(order);

        verify(kafkaTemplate2)
                .send(
                        eq("order-cancelled-topic"),
                        any(OrderCancelledEvent.class)
                );
    }

    @Test
    void shouldRejectCancellationForAnotherUser() {

        String owner = "owner@gmail.com";
        String attacker = "attacker@gmail.com";

        UUID orderId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .userEmail(owner)
                .status(OrderStatus.PLACED)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.cancelOrder(attacker, orderId)
        )
                .isInstanceOf(InvalidCredentialsException.class);

        verify(orderRepository, never())
                .save(any(Order.class));

        verify(kafkaTemplate2, never())
                .send(anyString(), any(OrderCancelledEvent.class));
    }

    @Test
    void shouldRejectCancellationForShippedOrder() {

        String email = "rahul@gmail.com";

        UUID orderId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .userEmail(email)
                .status(OrderStatus.SHIPPED)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.cancelOrder(email, orderId)
        )
                .isInstanceOf(InvalidOrderStateException.class);

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldMovePlacedOrderToConfirmed() {

        UUID orderId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PLACED)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        OrderResponse response =
                orderService.updateOrderStatus(
                        orderId,
                        OrderStatus.CONFIRMED
                );

        assertThat(response.getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldReturnMyOrders() {

        String email = "rahul@gmail.com";

        Order order1 = Order.builder()
                .id(UUID.randomUUID())
                .userEmail(email)
                .items(new ArrayList<>())
                .totalAmount(new BigDecimal("500"))
                .status(OrderStatus.PLACED)
                .build();

        Order order2 = Order.builder()
                .id(UUID.randomUUID())
                .userEmail(email)
                .items(new ArrayList<>())
                .totalAmount(new BigDecimal("1000"))
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findByUserEmail(email))
                .thenReturn(List.of(order1, order2));

        List<OrderResponse> response =
                orderService.getMyOrders(email);

        assertThat(response).hasSize(2);

        assertThat(response.get(0).getId())
                .isEqualTo(order1.getId());

        assertThat(response.get(1).getId())
                .isEqualTo(order2.getId());

        verify(orderRepository)
                .findByUserEmail(email);
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {

        UUID orderId = UUID.randomUUID();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.getOrderById(orderId)
        )
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository)
                .findById(orderId);
    }

    @Test
    void shouldRejectInvalidOrderStatusTransition() {

        UUID orderId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PLACED)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.updateOrderStatus(
                        orderId,
                        OrderStatus.SHIPPED
                )
        )
                .isInstanceOf(InvalidOrderStateException.class);

        verify(orderRepository, never())
                .save(any(Order.class));
    }
}