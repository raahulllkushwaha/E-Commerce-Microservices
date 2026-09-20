package com.rahul.paymentservice.payment;

import com.rahul.paymentservice.payment.dto.PaymentRequest;
import com.rahul.paymentservice.payment.dto.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse processPayment(String userEmail, PaymentRequest request);
    PaymentResponse getPaymentByOrderId(UUID orderId);
}
