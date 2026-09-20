package com.rahul.paymentservice.payment;

import com.rahul.paymentservice.common.exception.PaymentFailedException;
import com.rahul.paymentservice.common.exception.ResourceNotFoundException;
import com.rahul.paymentservice.payment.dto.PaymentRequest;
import com.rahul.paymentservice.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResponse processPayment(String userEmail, PaymentRequest request) {
        boolean isSuccess = Math.random() < 0.9; // 90% success simulate

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userEmail(userEmail)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .transactionId(UUID.randomUUID().toString())
                .build();

        Payment saved = paymentRepository.save(payment);

        if (!isSuccess) {
            throw new PaymentFailedException("Payment failed, please try again");
        }

        return mapToResponse(saved);
    }

    @Override
    public PaymentResponse getPaymentByOrderId(UUID orderId){
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .build();
    }
}
