package com.skybooker.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProcessPaymentRequest {

    @NotNull
    private UUID paymentId;

    @NotBlank
    private String razorpayPaymentId;

    @NotBlank
    private String razorpayOrderId;

    @NotBlank
    private String razorpaySignature;

    private String gatewayResponse;
    
    @NotNull
    private Boolean success;
}