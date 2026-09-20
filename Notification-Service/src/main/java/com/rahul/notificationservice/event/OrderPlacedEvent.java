package com.rahul.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    private UUID orderId;
    private String userEmail;
    private BigDecimal totalAmount;
}
