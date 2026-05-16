package com.skybooker.flight.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class FlightRequest {

    @NotBlank
    private String flightNumber;

    @NotNull
    private UUID airlineId;

    @NotBlank
    @Size(min = 3, max = 3)
    private String originAirportCode;

    @NotBlank
    @Size(min = 3, max = 3)
    private String destinationAirportCode;

    @NotNull
    private LocalDateTime departureTime;

    @NotNull
    private LocalDateTime arrivalTime;

    @NotBlank
    private String aircraftType;

    @NotNull
    @Min(1)
    private Integer totalSeats;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal basePrice;
}