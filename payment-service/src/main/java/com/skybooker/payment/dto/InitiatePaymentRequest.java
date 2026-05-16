package com.skybooker.payment.dto;

import com.skybooker.payment.entity.PaymentMode;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class InitiatePaymentRequest {

    @NotNull
    private UUID bookingId;

    @NotNull
    private UUID userId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private PaymentMode paymentMode;
    
    @NotBlank
    @Email
    private String contactEmail;
}