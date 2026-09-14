package com.rahul.orderservice.cart;

import com.rahul.orderservice.cart.dto.AddToCartRequest;
import com.rahul.orderservice.cart.dto.CartItemResponse;
import com.rahul.orderservice.cart.dto.CartResponse;
import com.rahul.orderservice.client.ProductResponse;
import com.rahul.orderservice.client.ProductServiceClient;
import com.rahul.orderservice.common.ApiResponse;
import com.rahul.orderservice.common.exception.InsufficientStockException;
import com.rahul.orderservice.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService{

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;

    @Override
    public CartResponse addToCart(String userEmail, AddToCartRequest request) {
        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseGet(() -> Cart.builder().userEmail(userEmail).build());

        ApiResponse<ProductResponse> productApiResponse = productServiceClient.getProductById(request.getProductId());
        ProductResponse product = productApiResponse.getData();

        if(!product.isActive()){
            throw new ResourceNotFoundException("Product not available");
        }
        if(product.getStockQuantity() < request.getQuantity()){
            throw new InsufficientStockException("Not enough stock available");
        }

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if(existingItem.isPresent()){
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        }
        else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return mapToCartResponse(savedCart);
    }

    @Override
    public CartResponse getCart(String userEmail) {
        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseGet(() -> Cart.builder().userEmail(userEmail).build());

        return mapToCartResponse(cart);
    }

    @Override
    public CartResponse removeCart(String userEmail, UUID itemId) {
        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userEmail));

       boolean isRemoved = cart.getItems().removeIf(item -> item.getId().equals(itemId));

       if(!isRemoved){
           throw new ResourceNotFoundException("Item with ID " + itemId + " not found in the cart");
       }
       Cart savedCart = cartRepository.save(cart);
       return mapToCartResponse(savedCart);
    }

    @Override
    public CartResponse updateItemQuantity(String userEmail, UUID itemId, Integer quantity) {
        if(quantity < 1){
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userEmail));

        CartItem itemToUpdate = cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item with ID " + itemId + " not found in the cart"));

        itemToUpdate.setQuantity(quantity);
        Cart savedCart = cartRepository.save(cart);
        return mapToCartResponse(savedCart);
    }

    @Override
    public void clearCart(String userEmail) {
        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userEmail));

        cart.getItems().clear();
        cartRepository.save(cart);
    }



    // helper to map the data
    private CartResponse mapToCartResponse(Cart cart){
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> CartItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userEmail(cart.getUserEmail())
                .items(itemResponses)
                .totalAmount(total)
                .build();
    }
}
