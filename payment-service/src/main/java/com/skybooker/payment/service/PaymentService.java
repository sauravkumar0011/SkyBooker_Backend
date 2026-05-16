package com.skybooker.payment.service;

import com.skybooker.payment.dto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse initiatePayment(InitiatePaymentRequest request);

    PaymentResponse processPayment(ProcessPaymentRequest request);

    PaymentResponse refundPayment(RefundPaymentRequest request);

    PaymentResponse getPaymentByBooking(UUID bookingId);

    List<PaymentResponse> getPaymentsByUser(UUID userId);

    List<PaymentResponse> getPaymentsByStatus(String status);

    String getPaymentStatus(UUID paymentId);

    BigDecimal getRevenue(LocalDateTime start, LocalDateTime end);
}