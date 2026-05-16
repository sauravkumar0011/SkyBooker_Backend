package com.skybooker.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RefundPaymentRequest {

    @NotNull
    private UUID paymentId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal refundAmount;
}