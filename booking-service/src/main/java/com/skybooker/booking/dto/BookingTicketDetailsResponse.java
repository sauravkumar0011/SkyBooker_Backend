package com.skybooker.booking.dto;

import com.skybooker.booking.entity.BookingStatus;
import com.skybooker.booking.entity.TripType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingTicketDetailsResponse {
    private UUID bookingId;
    private UUID userId;
    private UUID flightId;
    private String pnrCode;
    private TripType tripType;
    private BookingStatus status;
    private List<UUID> seatIds;
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
    private UUID paymentId;
    private FlightDetails flight;
    private List<PassengerDetails> passengers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerDetails {
        private UUID passengerId;
        private UUID seatId;
        private String seatNumber;
        private String firstName;
        private String lastName;
        private String fullName;
        private String gender;
        private LocalDate dateOfBirth;
        private String passportNumber;
        private String nationality;
        private String ticketNumber;
    }
}
