package com.skybooker.flight.dto;

import com.skybooker.flight.entity.FlightStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FlightResponse {
    private UUID flightId;
    private String flightNumber;
    private UUID airlineId;
    private String originAirportCode;
    private String destinationAirportCode;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer durationMinutes;
    private FlightStatus status;
    private String aircraftType;
    private Integer totalSeats;
    private Integer availableSeats;
    private BigDecimal basePrice;
}