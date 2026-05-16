package com.skybooker.airline.service;

import com.skybooker.airline.dto.*;

import java.util.List;
import java.util.UUID;

public interface AirlineService {

    AirlineResponse createAirline(AirlineRequest request);

    AirlineResponse getAirlineById(UUID airlineId);

    AirlineResponse getAirlineByIata(String iataCode);

    List<AirlineResponse> getAllAirlines();

    List<AirlineResponse> getActiveAirlines();

    AirlineResponse updateAirline(UUID airlineId, AirlineRequest request);

    AirlineResponse deactivateAirline(UUID airlineId);

    AirportResponse createAirport(AirportRequest request);

    AirportResponse getAirportByIata(String iataCode);

    List<AirportResponse> searchAirports(String keyword);

    List<AirportResponse> getAirportsByCity(String city);

    List<AirportResponse> getAirportsByCountry(String country);

    AirportResponse updateAirport(UUID airportId, AirportRequest request);
}