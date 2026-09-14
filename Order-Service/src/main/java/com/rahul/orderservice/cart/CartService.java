package com.rahul.orderservice.cart;

import com.rahul.orderservice.cart.dto.AddToCartRequest;
import com.rahul.orderservice.cart.dto.CartResponse;

import java.util.UUID;

public interface CartService {
    CartResponse addToCart(String userEmail, AddToCartRequest request);
    CartResponse getCart(String userEmail);
    CartResponse removeCart(String userEmail, UUID itemId);
    CartResponse updateItemQuantity(String userEmail, UUID itemId, Integer quantity);
    void clearCart(String userEmail);
}
