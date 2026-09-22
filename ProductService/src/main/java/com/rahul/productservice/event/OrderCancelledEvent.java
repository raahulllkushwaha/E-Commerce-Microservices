package com.rahul.productservice.event;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderCancelledEvent {
    private UUID orderId;
    private List<CancelledItem> items;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CancelledItem {
        private UUID productId;
        private Integer quantity;
    }
}