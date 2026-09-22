package com.rahul.paymentservice.payment;

import com.rahul.paymentservice.common.exception.PaymentFailedException;
import com.rahul.paymentservice.common.exception.ResourceNotFoundException;
import com.rahul.paymentservice.payment.dto.PaymentRequest;
import com.rahul.paymentservice.payment.dto.PaymentResponse;
import com.rahul.paymentservice.payment.event.PaymentEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Spy
    @InjectMocks
    private PaymentServiceImpl paymentService;


    @Test
    void shouldProcessPaymentSuccessfully() {

        UUID orderId = UUID.randomUUID();

        PaymentRequest request = PaymentRequest.builder()
                .orderId(orderId)
                .amount(new BigDecimal("1500"))
                .paymentMethod("CARD")
                .build();

        when(paymentRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.empty());

        doReturn(true)
                .when(paymentService)
                .simulatePaymentSuccess();

        Payment savedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userEmail("user@gmail.com")
                .amount(new BigDecimal("1500"))
                .paymentMethod("CARD")
                .status(PaymentStatus.SUCCESS)
                .transactionId("TXN-123")
                .build();

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        PaymentResponse response =
                paymentService.processPayment(
                        "user@gmail.com",
                        request
                );

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getAmount())
                .isEqualByComparingTo("1500");
        assertThat(response.getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getTransactionId())
                .isEqualTo("TXN-123");

        verify(paymentRepository)
                .save(any(Payment.class));

        verify(kafkaTemplate)
                .send(
                        eq("payment-status-topic"),
                        any(PaymentEvent.class)
                );
    }


    @Test
    void shouldFailPaymentWhenPaymentSimulationFails() {

        UUID orderId = UUID.randomUUID();

        PaymentRequest request = PaymentRequest.builder()
                .orderId(orderId)
                .amount(new BigDecimal("1500"))
                .paymentMethod("CARD")
                .build();

        when(paymentRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.empty());

        doReturn(false)
                .when(paymentService)
                .simulatePaymentSuccess();

        Payment failedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userEmail("user@gmail.com")
                .amount(new BigDecimal("1500"))
                .paymentMethod("CARD")
                .status(PaymentStatus.FAILED)
                .transactionId("TXN-FAILED")
                .build();

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(failedPayment);

        assertThrows(
                PaymentFailedException.class,
                () -> paymentService.processPayment(
                        "user@gmail.com",
                        request
                )
        );

        verify(paymentRepository)
                .save(any(Payment.class));

        verify(kafkaTemplate)
                .send(
                        eq("payment-status-topic"),
                        any(PaymentEvent.class)
                );
    }


    @Test
    void shouldReturnExistingSuccessfulPayment() {

        UUID orderId = UUID.randomUUID();

        Payment existingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userEmail("user@gmail.com")
                .amount(new BigDecimal("1500"))
                .paymentMethod("CARD")
                .status(PaymentStatus.SUCCESS)
                .transactionId("TXN-EXISTING")
                .build();

        when(paymentRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(existingPayment));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(orderId)
                .amount(new BigDecimal("1500"))
                .paymentMethod("CARD")
                .build();

        PaymentResponse response =
                paymentService.processPayment(
                        "user@gmail.com",
                        request
                );

        assertThat(response).isNotNull();
        assertThat(response.getId())
                .isEqualTo(existingPayment.getId());
        assertThat(response.getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getTransactionId())
                .isEqualTo("TXN-EXISTING");

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(kafkaTemplate, never())
                .send(anyString(), any(PaymentEvent.class));
    }


    @Test
    void shouldGetPaymentByOrderIdSuccessfully() {

        UUID orderId = UUID.randomUUID();

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userEmail("user@gmail.com")
                .amount(new BigDecimal("2500"))
                .paymentMethod("UPI")
                .status(PaymentStatus.SUCCESS)
                .transactionId("TXN-UPI-123")
                .build();

        when(paymentRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(payment));

        PaymentResponse response =
                paymentService.getPaymentByOrderId(orderId);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId())
                .isEqualTo(orderId);
        assertThat(response.getAmount())
                .isEqualByComparingTo("2500");
        assertThat(response.getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getTransactionId())
                .isEqualTo("TXN-UPI-123");
    }


    @Test
    void shouldThrowExceptionWhenPaymentDoesNotExist() {

        UUID orderId = UUID.randomUUID();

        when(paymentRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.getPaymentByOrderId(orderId)
        );
    }
}