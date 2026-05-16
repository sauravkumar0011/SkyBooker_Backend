package com.skybooker.payment.dto.external;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class BookingTicketDetailsResponse {
    private UUID bookingId;
    private UUID userId;
    private UUID flightId;
    private String pnrCode;
    private String status;
    private Integer totalPassengers;
    private BigDecimal baseFare;
    private BigDecimal taxes;
    private BigDecimal baggageCharge;
    private BigDecimal mealCharge;
    private BigDecimal totalFare;
    private String mealPreference;
    private Integer luggageKg;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime bookedAt;
    private FlightDetails flight;
    private List<PassengerDetails> passengers;

    @Data
    public static class FlightDetails {
        private UUID flightId;
        private String flightNumber;
        private String originAirportCode;
        private String destinationAirportCode;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private BigDecimal basePrice;
    }

    @Data
    public static class PassengerDetails {
        private UUID passengerId;
        private UUID seatId;
        private String seatNumber;
        private String firstName;
        private String lastName;
        private String fullName;
        private String gender;
        private String passportNumber;
        private String nationality;
        private String ticketNumber;
    }
}
