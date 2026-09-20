package com.rahul.notificationservice.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private UUID orderId;
    private String status;
}