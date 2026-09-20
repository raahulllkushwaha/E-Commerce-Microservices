package com.rahul.orderservice.cart;

import com.rahul.orderservice.cart.dto.AddToCartRequest;
import com.rahul.orderservice.cart.dto.CartResponse;
import com.rahul.orderservice.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(@Valid @RequestBody AddToCartRequest request){
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        CartResponse cartResponse = cartService.addToCart(userEmail, request);

        ApiResponse<CartResponse> response = ApiResponse.<CartResponse>builder()
                .success(true)
                .message("Item added to cart successfully")
                .data(cartResponse)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        CartResponse cartData = cartService.getCart(userEmail);

        ApiResponse<CartResponse> response = ApiResponse.<CartResponse>builder()
                .success(true)
                .message("Cart fetched successfully")
                .data(cartData)
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCart(@PathVariable UUID itemId) {

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        CartResponse cartData = cartService.removeCart(userEmail, itemId); //[cite: 5]

        ApiResponse<CartResponse> response = ApiResponse.<CartResponse>builder()
                .success(true)
                .message("Item removed from cart successfully")
                .data(cartData)
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable UUID itemId,
            @RequestParam Integer quantity) {

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        CartResponse cartData = cartService.updateItemQuantity(userEmail, itemId, quantity); //[cite: 5]

        ApiResponse<CartResponse> response = ApiResponse.<CartResponse>builder()
                .success(true)
                .message("Cart item quantity updated successfully")
                .data(cartData)
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userEmail}")
    public ResponseEntity<ApiResponse<Void>> clearCart() {

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.clearCart(userEmail);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Cart cleared successfully")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }


}
