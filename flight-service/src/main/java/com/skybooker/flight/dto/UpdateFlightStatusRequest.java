package com.skybooker.flight.dto;

import com.skybooker.flight.entity.FlightStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateFlightStatusRequest {
	
    @NotNull
    private FlightStatus status;
}