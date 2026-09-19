package com.rahul.orderservice.order;

import com.rahul.orderservice.common.ApiResponse;
import com.rahul.orderservice.order.dto.OrderResponse;
import com.rahul.orderservice.order.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody PlaceOrderRequest request){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        OrderResponse orderResponse = orderService.placeOrder(email, request);

        ApiResponse<OrderResponse> response = ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order placed successfully")
                .data(orderResponse)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        List<OrderResponse> orders = orderService.getMyOrders(email);

        ApiResponse<List<OrderResponse>> response = ApiResponse.<List<OrderResponse>>builder()
                .success(true)
                .message("All orders")
                .data(orders)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        OrderResponse orderResponse = orderService.getOrderById(id);

        ApiResponse<OrderResponse> response = ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order placed successfully")
                .data(orderResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        OrderResponse orderResponse = orderService.cancelOrder(email, id);
        ApiResponse<OrderResponse> response = ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order Cancelled")
                .data(orderResponse)
                .build();

        return ResponseEntity.ok(response);

    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id, @RequestParam OrderStatus status) {

        OrderResponse response = orderService.updateOrderStatus(id, status);

        ApiResponse<OrderResponse> apiResponse = ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order status updated")
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }
}
