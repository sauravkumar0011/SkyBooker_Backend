package com.skybooker.payment.service;

import com.razorpay.RazorpayClient;
import com.skybooker.payment.client.BookingClient;
import com.skybooker.payment.dto.InitiatePaymentRequest;
import com.skybooker.payment.dto.PaymentResponse;
import com.skybooker.payment.dto.ProcessPaymentRequest;
import com.skybooker.payment.dto.RefundPaymentRequest;
import com.skybooker.payment.dto.external.BookingTicketDetailsResponse;
import com.skybooker.payment.entity.Payment;
import com.skybooker.payment.entity.PaymentMode;
import com.skybooker.payment.entity.PaymentStatus;
import com.skybooker.payment.messaging.PaymentEventPublisher;
import com.skybooker.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingClient bookingClient;

    @Mock
    private PaymentEventPublisher publisher;

    @Mock
    private RazorpayClient razorpayClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID paymentId;
    private UUID bookingId;
    private UUID userId;
    private Payment payment;
    private BookingTicketDetailsResponse booking;

    @BeforeEach
    void setup() {
        paymentId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();

        payment = Payment.builder()
                .paymentId(paymentId)
                .bookingId(bookingId)
                .userId(userId)
                .amount(BigDecimal.valueOf(5000))
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .paymentMode(PaymentMode.UPI)
                .razorpayOrderId("order_123")
                .contactEmail("test@mail.com")
                .build();

        booking = buildBooking();
    }

    @Test
    void initiatePayment_success() throws Exception {
        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setBookingId(bookingId);
        req.setUserId(userId);
        req.setAmount(BigDecimal.valueOf(5000));
        req.setCurrency("INR");
        req.setPaymentMode(PaymentMode.UPI);
        req.setContactEmail("test@mail.com");

        com.razorpay.Order order = Mockito.mock(com.razorpay.Order.class);
        when(order.get("id")).thenReturn("order_123");

        com.razorpay.OrderClient orderClient = Mockito.mock(com.razorpay.OrderClient.class);
        when(orderClient.create(any())).thenReturn(order);

        razorpayClient.orders = orderClient;

        when(bookingClient.getBookingTicketDetails(bookingId)).thenReturn(booking);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setPaymentId(paymentId);
            return saved;
        });

        PaymentResponse response = paymentService.initiatePayment(req);

        assertNotNull(response);
        assertEquals(paymentId, response.getPaymentId());
        assertEquals("order_123", response.getRazorpayOrderId());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals(BigDecimal.valueOf(5000), response.getAmount());

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void processPayment_invalidOrderId() {
        ProcessPaymentRequest req = new ProcessPaymentRequest();
        req.setPaymentId(paymentId);
        req.setRazorpayOrderId("wrong");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThrows(RuntimeException.class,
                () -> paymentService.processPayment(req));
    }

    @Test
    void processPayment_signatureInvalid() throws Exception {
        ProcessPaymentRequest req = new ProcessPaymentRequest();
        req.setPaymentId(paymentId);
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        req.setRazorpaySignature("sig");
        req.setSuccess(true);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<com.razorpay.Utils> utils = Mockito.mockStatic(com.razorpay.Utils.class)) {
            utils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(), any()))
                    .thenReturn(false);

            assertThrows(RuntimeException.class,
                    () -> paymentService.processPayment(req));
        }

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(bookingClient).cancelBooking(bookingId);
    }

    @Test
    void processPayment_success() throws Exception {
        ProcessPaymentRequest req = new ProcessPaymentRequest();
        req.setPaymentId(paymentId);
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        req.setRazorpaySignature("sig");
        req.setSuccess(true);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingClient.getBookingTicketDetails(bookingId)).thenReturn(booking);

        try (MockedStatic<com.razorpay.Utils> utils = Mockito.mockStatic(com.razorpay.Utils.class)) {
            utils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(), any()))
                    .thenReturn(true);

            PaymentResponse response = paymentService.processPayment(req);

            assertEquals(PaymentStatus.PAID, response.getStatus());
            verify(bookingClient).confirmBooking(bookingId);
            verify(publisher).publishPaymentSuccess(any());
        }
    }

    @Test
    void refundPayment_success() {
        payment.setStatus(PaymentStatus.PAID);

        RefundPaymentRequest req = new RefundPaymentRequest();
        req.setPaymentId(paymentId);
        req.setRefundAmount(BigDecimal.valueOf(2000));

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.refundPayment(req);

        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        verify(bookingClient).cancelBooking(bookingId);
        verify(publisher).publishRefundEvent(any());
    }

    @Test
    void refundPayment_notPaid_throwException() {
        payment.setStatus(PaymentStatus.FAILED);

        RefundPaymentRequest req = new RefundPaymentRequest();
        req.setPaymentId(paymentId);
        req.setRefundAmount(BigDecimal.valueOf(1000));

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThrows(RuntimeException.class,
                () -> paymentService.refundPayment(req));
    }

    @Test
    void getPaymentByBooking_success() {
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));

        PaymentResponse res = paymentService.getPaymentByBooking(bookingId);

        assertEquals(bookingId, res.getBookingId());
    }

    @Test
    void getPaymentsByUser_success() {
        when(paymentRepository.findByUserId(userId)).thenReturn(List.of(payment));

        List<PaymentResponse> list = paymentService.getPaymentsByUser(userId);

        assertEquals(1, list.size());
    }

    @Test
    void getPaymentStatus_success() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        String status = paymentService.getPaymentStatus(paymentId);

        assertEquals("PENDING", status);
    }

    @Test
    void getRevenue_success() {
        payment.setStatus(PaymentStatus.PAID);

        when(paymentRepository.findByPaidAtBetween(any(), any()))
                .thenReturn(List.of(payment));

        BigDecimal revenue = paymentService.getRevenue(LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertEquals(BigDecimal.valueOf(5000), revenue);
    }

    private BookingTicketDetailsResponse buildBooking() {
        BookingTicketDetailsResponse booking = new BookingTicketDetailsResponse();
        booking.setBookingId(bookingId);
        booking.setUserId(userId);
        booking.setPnrCode("ABC123");
        booking.setStatus("PENDING");
        booking.setTotalPassengers(1);
        booking.setTotalFare(BigDecimal.valueOf(5000));
        booking.setContactEmail("test@mail.com");
        booking.setContactPhone("9999999999");
        booking.setBookedAt(LocalDateTime.now());

        BookingTicketDetailsResponse.FlightDetails flight = new BookingTicketDetailsResponse.FlightDetails();
        flight.setFlightId(UUID.randomUUID());
        flight.setFlightNumber("SB101");
        flight.setOriginAirportCode("DEL");
        flight.setDestinationAirportCode("BOM");
        flight.setDepartureTime(LocalDateTime.now().plusDays(1));
        flight.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        booking.setFlight(flight);

        BookingTicketDetailsResponse.PassengerDetails passenger = new BookingTicketDetailsResponse.PassengerDetails();
        passenger.setPassengerId(UUID.randomUUID());
        passenger.setSeatId(UUID.randomUUID());
        passenger.setSeatNumber("12A");
        passenger.setFirstName("Himanshu");
        passenger.setLastName("Kumar");
        passenger.setFullName("Himanshu Kumar");
        passenger.setPassportNumber("ABC12345");
        passenger.setNationality("Indian");
        passenger.setTicketNumber("TKT-12345678");
        booking.setPassengers(List.of(passenger));

        return booking;
    }
}
