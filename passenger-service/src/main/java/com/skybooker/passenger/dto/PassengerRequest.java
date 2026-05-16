package com.skybooker.passenger.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PassengerRequest {

    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    @NotEmpty(message = "At least one passenger is required")
    private List<@Valid PassengerDetailsRequest> passengers;
}
