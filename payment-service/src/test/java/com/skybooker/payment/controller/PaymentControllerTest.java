package com.skybooker.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.payment.dto.*;
import com.skybooker.payment.entity.PaymentMode;
import com.skybooker.payment.entity.PaymentStatus;
import com.skybooker.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PaymentService paymentService;

    private UUID paymentId;

    @BeforeEach
    void setup() {
        PaymentController controller = new PaymentController(paymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        paymentId = UUID.randomUUID();
    }

    private InitiatePaymentRequest validInitReq() {
        InitiatePaymentRequest r = new InitiatePaymentRequest();
        r.setBookingId(UUID.randomUUID());
        r.setUserId(UUID.randomUUID());
        r.setAmount(BigDecimal.valueOf(5000));
        r.setCurrency("INR");
        r.setPaymentMode(PaymentMode.UPI);
        r.setContactEmail("test@mail.com");
        return r;
    }

    @Test
    void initiatePayment_success() throws Exception {
        when(paymentService.initiatePayment(any()))
                .thenReturn(PaymentResponse.builder().status(PaymentStatus.PENDING).build());

        mockMvc.perform(post("/payments/initiate")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInitReq())))
                .andExpect(status().isCreated());
    }

    @Test
    void processPayment_success() throws Exception {
        ProcessPaymentRequest req = new ProcessPaymentRequest();
        req.setPaymentId(paymentId);
        req.setRazorpayOrderId("order");
        req.setRazorpayPaymentId("pay");
        req.setRazorpaySignature("sig");
        req.setSuccess(true);

        when(paymentService.processPayment(any()))
                .thenReturn(PaymentResponse.builder().status(PaymentStatus.PAID).build());

        mockMvc.perform(post("/payments/process")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void refundPayment_success() throws Exception {
        RefundPaymentRequest req = new RefundPaymentRequest();
        req.setPaymentId(paymentId);
        req.setRefundAmount(BigDecimal.valueOf(1000));

        when(paymentService.refundPayment(any()))
                .thenReturn(PaymentResponse.builder().status(PaymentStatus.REFUNDED).build());

        mockMvc.perform(post("/payments/refund")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void getPaymentByBooking_success() throws Exception {
        when(paymentService.getPaymentByBooking(any()))
                .thenReturn(PaymentResponse.builder().build());

        mockMvc.perform(get("/payments/booking/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void getPaymentsByUser_success() throws Exception {
        when(paymentService.getPaymentsByUser(any()))
                .thenReturn(List.of(PaymentResponse.builder().build()));

        mockMvc.perform(get("/payments/user/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void getPaymentStatus_success() throws Exception {
        when(paymentService.getPaymentStatus(paymentId)).thenReturn("PAID");

        mockMvc.perform(get("/payments/{id}/payment-status", paymentId))
                .andExpect(status().isOk())
                .andExpect(content().string("PAID"));
    }
}