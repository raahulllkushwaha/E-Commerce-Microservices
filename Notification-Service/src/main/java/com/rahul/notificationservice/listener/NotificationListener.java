package com.rahul.notificationservice.listener;

import com.rahul.notificationservice.event.OrderPlacedEvent;
import com.rahul.notificationservice.event.PaymentEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    @KafkaListener(topics = "order-placed-topic", containerFactory = "orderKafkaListenerFactory")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        System.out.println("Sending email to " + event.getUserEmail() +
                " — Order " + event.getOrderId() + " placed, total: " + event.getTotalAmount());
    }

    @KafkaListener(topics = "payment-status-topic", containerFactory = "paymentKafkaListenerFactory")
    public void handlePaymentStatus(PaymentEvent event) {
        System.out.println("Payment " + event.getStatus() + " for order " + event.getOrderId());
    }
}