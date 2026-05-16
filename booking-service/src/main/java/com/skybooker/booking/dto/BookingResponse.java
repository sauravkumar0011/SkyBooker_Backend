package com.skybooker.booking.dto;

import com.skybooker.booking.entity.BookingStatus;
import com.skybooker.booking.entity.TripType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {
    private UUID bookingId;
    private UUID userId;
    private UUID flightId;
    private List<UUID> seatIds;
    private String pnrCode;
    private TripType tripType;
    private BookingStatus status;
    private BigDecimal totalFare;
    private BigDecimal baseFare;
    private BigDecimal taxes;
    private Integer totalPassengers;
    private String mealPreference;
    private Integer luggageKg;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime bookedAt;
    private UUID paymentId;
}
