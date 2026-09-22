package com.rahul.productservice.listener;

import com.rahul.productservice.event.OrderCancelledEvent;
import com.rahul.productservice.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final ProductService productService;

    @KafkaListener(topics = "order-cancelled-topic", containerFactory = "orderCancelledFactory")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        for (OrderCancelledEvent.CancelledItem item : event.getItems()) {
            productService.increaseStock(item.getProductId(), item.getQuantity());
        }
    }
}