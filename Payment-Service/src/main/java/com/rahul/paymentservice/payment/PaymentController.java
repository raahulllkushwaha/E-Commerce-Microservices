package com.rahul.paymentservice.payment;

import com.rahul.paymentservice.common.ApiResponse;
import com.rahul.paymentservice.payment.dto.PaymentRequest;
import com.rahul.paymentservice.payment.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@Valid @RequestBody PaymentRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        PaymentResponse paymentResponse = paymentService.processPayment(email, request);

        ApiResponse<PaymentResponse> response = ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment Successful")
                .data(paymentResponse)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByOrderId(@PathVariable UUID orderId) {
        PaymentResponse paymentResponse = paymentService.getPaymentByOrderId(orderId);

        ApiResponse<PaymentResponse> response = ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payments")
                .data(paymentResponse)
                .build();

        return ResponseEntity.ok(response);
    }
}
