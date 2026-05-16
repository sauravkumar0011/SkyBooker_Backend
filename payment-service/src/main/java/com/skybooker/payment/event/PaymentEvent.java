package com.skybooker.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String eventType;
    private UUID paymentId;
    private UUID bookingId;
    private UUID userId;
    private BigDecimal amount;
    private String status;
    private String transactionId;
    private String recipientEmail;
    private String contactPhone;
    private String pnrCode;
    private Integer passengerCount;
    private List<String> passengerNames;
    private List<PassengerTicketInfo> passengers;
    private String flightNumber;
    private String originAirportCode;
    private String destinationAirportCode;
    private String departureTime;
    private String arrivalTime;
    private String bookedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerTicketInfo {
        private String fullName;
        private String seatNumber;
        private String ticketNumber;
        private String passportNumber;
        private String nationality;
    }
}
