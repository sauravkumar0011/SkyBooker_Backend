package com.skybooker.airline.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AirportResponse {
    private UUID airportId;
    private String name;
    private String iataCode;
    private String icaoCode;
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;
    private String timezone;
}