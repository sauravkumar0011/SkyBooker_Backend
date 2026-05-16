package com.skybooker.airline.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AirlineResponse {
    private UUID airlineId;
    private String name;
    private String iataCode;
    private String icaoCode;
    private String country;
    private String contactEmail;
    private String contactPhone;
    private Boolean isActive;
}