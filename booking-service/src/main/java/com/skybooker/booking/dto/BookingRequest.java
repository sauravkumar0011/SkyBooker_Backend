package com.skybooker.booking.dto;

import com.skybooker.booking.entity.TripType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BookingRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID flightId;

    @NotEmpty
    private List<@NotNull UUID> seatIds;

    @NotNull
    private TripType tripType;

    private String mealPreference;

    @Min(0)
    private Integer luggageKg;

    @NotBlank
    @Email
    private String contactEmail;

    @NotBlank
    private String contactPhone;
}
