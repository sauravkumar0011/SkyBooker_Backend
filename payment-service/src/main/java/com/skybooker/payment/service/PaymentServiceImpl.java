package com.skybooker.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.skybooker.payment.client.BookingClient;
import com.skybooker.payment.dto.*;
import com.skybooker.payment.dto.external.BookingTicketDetailsResponse;
import com.skybooker.payment.entity.Payment;
import com.skybooker.payment.entity.PaymentStatus;
import com.skybooker.payment.event.PaymentEvent;
import com.skybooker.payment.messaging.PaymentEventPublisher;
import com.skybooker.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final BookingClient bookingClient;
    private final PaymentEventPublisher paymentEventPublisher;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String razorpayKey;

    @Value("${razorpay.key-secret}")
    private String razorpaySecret;

    //CREATE RAZORPAY ORDER
    @Override
    public PaymentResponse initiatePayment(InitiatePaymentRequest request) {
        try {
            BookingTicketDetailsResponse booking = bookingClient.getBookingTicketDetails(request.getBookingId());
            validatePaymentInitiation(request, booking);

            int amountInPaise = request.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();

            JSONObject orderReq = new JSONObject();
            orderReq.put("amount", amountInPaise);
            orderReq.put("currency", request.getCurrency());
            
            String receipt = "bk_" + request.getBookingId().toString().replace("-", "").substring(0, 20);
            orderReq.put("receipt", receipt);

            Order order = razorpayClient.orders.create(orderReq);

            Payment payment = paymentRepository.findByBookingId(request.getBookingId())
                    .orElseGet(() -> Payment.builder()
                            .bookingId(request.getBookingId())
                            .userId(request.getUserId())
                            .build());

            if (payment.getStatus() == PaymentStatus.PAID) {
                throw new RuntimeException("Payment has already been completed for this booking");
            }

            payment.setAmount(request.getAmount());
            payment.setCurrency(request.getCurrency());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentMode(request.getPaymentMode());
            payment.setRazorpayOrderId(order.get("id"));
            payment.setContactEmail(booking.getContactEmail() != null && !booking.getContactEmail().isBlank()
                    ? booking.getContactEmail()
                    : request.getContactEmail());
            payment.setGatewayResponse(null);
            payment.setRazorpayPaymentId(null);
            payment.setRazorpaySignature(null);
            payment.setTransactionId(null);
            payment.setPaidAt(null);
            payment.setRefundAmount(null);
            payment.setRefundedAt(null);

            Payment saved = paymentRepository.save(payment);

            return PaymentResponse.builder()
                    .paymentId(saved.getPaymentId())
                    .bookingId(saved.getBookingId())
                    .userId(saved.getUserId())
                    .amount(saved.getAmount())
                    .currency(saved.getCurrency())
                    .status(saved.getStatus())
                    .paymentMode(saved.getPaymentMode())
                    .razorpayOrderId(saved.getRazorpayOrderId())
                    .razorpayKey(razorpayKey)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Razorpay order creation failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse processPayment(ProcessPaymentRequest request) {

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        boolean paymentCaptured = false;
        
        if (!payment.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
            throw new RuntimeException("Invalid order ID");
        }

        try {
            if (request.getSuccess() == null) {
                throw new RuntimeException("Payment success flag is required");
            }
            
            if (!request.getSuccess()) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setTransactionId(request.getRazorpayPaymentId());
                payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
                payment.setRazorpaySignature(request.getRazorpaySignature());
                payment.setGatewayResponse(request.getGatewayResponse());
                paymentRepository.save(payment);
                cancelBookingQuietly(payment.getBookingId(), "payment reported failure");
                return map(payment);
            }

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, razorpaySecret);

            logger.info("Signature Valid: {}", isValid);
            
            if (!isValid) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new RuntimeException("Invalid Razorpay signature");
            }
            
            payment.setStatus(PaymentStatus.PAID);
            payment.setTransactionId(request.getRazorpayPaymentId());
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            payment.setGatewayResponse(request.getGatewayResponse());
            payment.setPaidAt(LocalDateTime.now());

            Payment saved = paymentRepository.save(payment);
            paymentCaptured = true;
            
            bookingClient.confirmBooking(saved.getBookingId());
            BookingTicketDetailsResponse booking = bookingClient.getBookingTicketDetails(saved.getBookingId());

            try {
            paymentEventPublisher.publishPaymentSuccess(
                    buildPaymentSuccessEvent(saved, booking)
            );
            }catch(Exception mqEx) {
            	 logger.error("Payment success event publish failed: {}", mqEx.getMessage());
            }

            return map(saved);

        } catch (Exception e) {
            logger.error("Payment verification failed: {}", e.getMessage(), e);

            if (paymentCaptured) {
                payment.setGatewayResponse("Payment captured but booking finalization failed: " + e.getMessage());
                paymentRepository.save(payment);
                throw new RuntimeException("Payment captured, but booking finalization failed: " + e.getMessage());
            }

            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse(e.getMessage());
            paymentRepository.save(payment);
            cancelBookingQuietly(payment.getBookingId(), "payment verification failure");

            throw new RuntimeException("Payment verification failed: " + e.getMessage());
        }
    }

    //REFUND (NO CHANGE)
    @Override
    public PaymentResponse refundPayment(RefundPaymentRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("Only paid payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundAmount(request.getRefundAmount());
        payment.setRefundedAt(LocalDateTime.now());

        Payment updated = paymentRepository.save(payment);

        bookingClient.cancelBooking(payment.getBookingId());

        paymentEventPublisher.publishRefundEvent(
                PaymentEvent.builder()
                        .eventType("PAYMENT_REFUND")
                        .paymentId(updated.getPaymentId())
                        .bookingId(updated.getBookingId())
                        .userId(updated.getUserId())
                        .amount(updated.getRefundAmount())
                        .status(updated.getStatus().name())
                        .transactionId(updated.getTransactionId())
                        .recipientEmail(updated.getContactEmail())
                        .build()
        );

        return map(updated);
    }

    @Override
    public PaymentResponse getPaymentByBooking(UUID bookingId) {
        return map(paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found")));
    }

    @Override
    public List<PaymentResponse> getPaymentsByUser(UUID userId) {
        return paymentRepository.findByUserId(userId)
                .stream().map(this::map).toList();
    }

    @Override
    public List<PaymentResponse> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(PaymentStatus.valueOf(status.toUpperCase()))
                .stream().map(this::map).toList();
    }

    @Override
    public String getPaymentStatus(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"))
                .getStatus().name();
    }

    @Override
    public BigDecimal getRevenue(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.findByPaidAtBetween(start, end)
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PaymentResponse map(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .bookingId(p.getBookingId())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .paymentMode(p.getPaymentMode())
                .transactionId(p.getTransactionId())
                .gatewayResponse(p.getGatewayResponse())
                .paidAt(p.getPaidAt())
                .refundedAt(p.getRefundedAt())
                .refundAmount(p.getRefundAmount())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .razorpayKey(razorpayKey)
                .build();
    }

    private void validatePaymentInitiation(InitiatePaymentRequest request, BookingTicketDetailsResponse booking) {
        if (booking == null) {
            throw new RuntimeException("Booking not found");
        }

        if (booking.getUserId() != null && !booking.getUserId().equals(request.getUserId())) {
            throw new RuntimeException("Booking does not belong to the requesting user");
        }

        if (!"PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Payment can only be initiated for pending bookings");
        }

        if (booking.getPassengers() == null || booking.getPassengers().size() != booking.getTotalPassengers()) {
            throw new RuntimeException("Passenger details must be completed before payment");
        }

        if (request.getAmount().compareTo(booking.getTotalFare()) != 0) {
            throw new RuntimeException("Payment amount must match the complete booking fare");
        }
    }

    private void cancelBookingQuietly(UUID bookingId, String reason) {
        try {
            bookingClient.cancelBooking(bookingId);
        } catch (Exception ex) {
            logger.error("Failed to cancel booking {} after {}: {}", bookingId, reason, ex.getMessage());
        }
    }

    private PaymentEvent buildPaymentSuccessEvent(Payment payment, BookingTicketDetailsResponse booking) {
        List<PaymentEvent.PassengerTicketInfo> passengers = booking.getPassengers().stream()
                .map(passenger -> PaymentEvent.PassengerTicketInfo.builder()
                        .fullName(passenger.getFullName())
                        .seatNumber(passenger.getSeatNumber())
                        .ticketNumber(passenger.getTicketNumber())
                        .passportNumber(passenger.getPassportNumber())
                        .nationality(passenger.getNationality())
                        .build())
                .toList();

        List<String> passengerNames = booking.getPassengers().stream()
                .map(BookingTicketDetailsResponse.PassengerDetails::getFullName)
                .toList();

        return PaymentEvent.builder()
                .eventType("PAYMENT_SUCCESS")
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .recipientEmail(payment.getContactEmail())
                .contactPhone(booking.getContactPhone())
                .pnrCode(booking.getPnrCode())
                .passengerCount(booking.getTotalPassengers())
                .passengerNames(passengerNames)
                .passengers(passengers)
                .flightNumber(booking.getFlight().getFlightNumber())
                .originAirportCode(booking.getFlight().getOriginAirportCode())
                .destinationAirportCode(booking.getFlight().getDestinationAirportCode())
                .departureTime(String.valueOf(booking.getFlight().getDepartureTime()))
                .arrivalTime(String.valueOf(booking.getFlight().getArrivalTime()))
                .bookedAt(String.valueOf(booking.getBookedAt()))
                .build();
    }
}
